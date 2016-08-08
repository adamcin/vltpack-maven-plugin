/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package net.adamcin.vltpack

import java.io.File
import java.nio.charset.Charset

import com.ning.http.client.Response
import com.ning.http.multipart.{FilePart, StringPart}
import dispatch._
import org.apache.jackrabbit.vault.packaging.PackageId
import org.apache.maven.artifact.Artifact
import org.apache.maven.plugin.{MojoExecutionException, MojoFailureException}
import org.apache.maven.plugins.annotations.Parameter
import org.slf4j.LoggerFactory

import scala.util.parsing.json.JSON

/**
 * Trait defining common mojo parameters and methods useful for uploading and installing vault packages on
 * CQ servers
 * @since 0.6.0
 * @author Mark Adamcin
 */
trait UploadsPackages extends HttpParameters with IdentifiesPackages {
  private val log = LoggerFactory.getLogger(getClass)

  /**
   * Force upload of packages if they already exist in the target environment
   */
  @Parameter(property = "vltpack.upload.force")
  implicit val force = false

  /**
   * Set to false to not install any subpackages that might be embedded within each dependency
   */
  @Parameter(property = "vltpack.upload.recursive", defaultValue = "true")
  val recursive = true

  /**
   * Change the autosave threshold for the install command. 1024 is the minimum.
   */
  @Parameter(property = "vltpack.upload.autosave", defaultValue = "1024")
  val autosave = 1024

  /**
   * Set the timeout used for waiting for Package Manager service availability before sending
   * any POST requests. The wait is to avoid creating dummy nodes in the repository when the SlingPostServlet
   * handles premature package manager requests, which can confuse the link rewriter and mess up
   * links to the package manager console.
   */
  @Parameter(property = "vltpack.upload.timeout", defaultValue = "60")
  val serviceTimeout = 60

  lazy val servicePath = "/crx/packmgr/service/exec.json"

  /**
   * wrap package manager service POST requests with this function to first check for service availability,
   * which is interpreted by expecting a GET response of 405 (Method not allowed)
   * @param thenDo
   * @return
   */
  def waitForService(thenDo: => Either[Throwable, (Boolean, String)]): Either[Throwable, (Boolean, String)] = {
    val until = System.currentTimeMillis() + (serviceTimeout * 1000)
    import dispatch._
    val responseHandler =
      (response: Response) => {
        if (response.getStatusCode == 405) {
          as.String(response)
        } else {
          throw StatusCode(response.getStatusCode)
        }
      }

    def checkContent(response: Future[String]): Future[Boolean] = {
      response.fold((ex) => { getLog.info("check service exception: " + ex.getMessage); false },
        (content) => true)
    }

    if (waitForResponse(0)(until, () => urlForPath(servicePath).subject > responseHandler, checkContent)) {
      thenDo
    } else {
      Left(new MojoFailureException("Package Manager service failed to respond as expected within " + serviceTimeout + " seconds."))
    }
  }

  def uploadPackage(packageId: Option[PackageId], file: File, force: Boolean): Either[Throwable, (Boolean, String)] = {
    waitForService {
      packageId match {
        case Some(id) => {

          var req = urlForPath(servicePath + id.getInstallationPath + s"?cmd=upload&force=$force").POST
          req = req.setContentType("multipart/form-data", "UTF-8")
          req = req.addBodyPart(new FilePart("package", id.getDownloadName, file))
          req = req.addBodyPart(new StringPart("cmd", "upload"))
          req = req.addBodyPart(new StringPart("force", force.toString))
          val resp = Http(req).apply()

          if (isSuccess(req, resp)) {
            parseServiceResponse(resp.getResponseBody)
          } else {
            Left(new MojoExecutionException("Failed to upload file: " + file))
          }
        }
        case None => Left(new MojoExecutionException("Failed to identify package"))
      }
    }
  }

  def installPackage(packageId: Option[PackageId]): Either[Throwable, (Boolean, String)] = {
    waitForService {
      packageId match {
        case Some(id) => {
          val pkgPath = id.getInstallationPath + ".zip"
          val req = urlForPath(servicePath + pkgPath) << Map(
            "cmd" -> "install",
            "recursive" -> recursive.toString,
            "autosave" -> (autosave max 1024).toString
          )
          val resp = Http(req).apply()

          if (isSuccess(req, resp)) {
            parseServiceResponse(resp.getResponseBody)
          } else {
            Left(new MojoExecutionException("Failed to install package at path: " + pkgPath))
          }
        }
        case None => Left(new MojoExecutionException("Failed to identify package"))
      }
    }
  }

  def existsOnServer(packageId: Option[PackageId]): Either[Throwable, (Boolean, String)] = {
    waitForService {
      packageId match {
        case Some(id) => {
          val pkgPath = id.getInstallationPath + ".zip"
          val req = urlForPath(servicePath + pkgPath) << Map("cmd" -> "contents")
          val resp = Http(req).apply()

          if (isSuccess(req, resp)) {
            parseServiceResponse(resp.getResponseBody)
          } else {
            Left(new MojoExecutionException("failed to check for existence of package on server: " + pkgPath))
          }
        }
        case None => Left(new MojoExecutionException("Failed to identify package"))
      }
    }
  }

  def parseServiceResponse(respBody: String): Either[Throwable, (Boolean, String)] = {
    import VltpackUtil._
    val json = List(JSON.parseFull(respBody))
    (for {
      Some(M(map)) <- json
      B(success) = map("success")
      S(msg) = map("msg")
    } yield (success, msg)) match {
      case head :: tail => Right(head)
      case _ => Left(new MojoExecutionException("failed to parse json response: " + respBody))
    }
  }

  def uploadPackageArtifact(artifact: Artifact)(implicit force: Boolean) {
    Option(artifact.getFile) match {
      case None => throw new MojoExecutionException("failed to resolve artifact: " + artifact.getId)
      case Some(file) => uploadPackageFile(file)
    }
  }

  def uploadPackageFile(file: File)(implicit force: Boolean) {
    val thrower = (t: Throwable) => throw t
    val id = identifyPackage(file)
    val doesntExist = force || (existsOnServer(id) fold (thrower, {
      (result) => {
        val (success, msg) = result
        val successMsg = if (success) "Package exists" else "Package not found"
        getLog.info("check for installed package " + id.get.getInstallationPath + ".zip: " + successMsg)
        !success
      }
    }))

    if (doesntExist) {
      val uploaded = uploadPackage(id, file, force) fold (thrower, {
        (result) => {
          val (success, msg) = result
          getLog.info("uploading " + file + " to " + id.get.getInstallationPath + ".zip: " + msg)
          success
        }
      })

      if (uploaded) {
        installPackage(id) fold (thrower, {
          (result) => {
            val (success, msg) = result
            getLog.info("installing " + id.get.getInstallationPath + ".zip: " + msg)
          }
        })
      } else {
        getLog.info("package was not uploaded and so it will not be installed")
      }
    }
  }
}