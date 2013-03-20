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
import org.apache.maven.plugin.MojoExecutionException
import dispatch._
import org.slf4j.LoggerFactory
import util.parsing.json.JSON
import com.ning.http.multipart.FilePart
import com.day.jcr.vault.packaging.PackageId
import org.apache.maven.plugins.annotations.Parameter

/**
 * Trait defining common mojo parameters and methods useful for uploading and installing vault packages on
 * CQ servers
 * @since 0.6.0
 * @author Mark Adamcin
 */
trait UploadsPackages extends HttpParameters {
  val log = LoggerFactory.getLogger(getClass)

  /**
   * Set to false to not install any subpackages that might be embedded within each dependency
   */
  @Parameter(defaultValue = "true")
  val recursive = true

  /**
   * Change the autosave threshold for the install command
   */
  @Parameter(defaultValue = "1024")
  val autosave = 1024

  lazy val servicePath = "/crx/packmgr/service/exec.json"

  def uploadPackage(packageId: Option[PackageId], file: File, force: Boolean): Either[Throwable, (Boolean, String)] = {
    packageId match {
      case Some(id) => {
        val req = urlForPath(servicePath + id.getInstallationPath).POST <<? Map(
          "cmd" -> "upload",
          "force" -> force.toString
        )
        req.addBodyPart(new FilePart("package", id.getDownloadName, file))
        val resp = Http(req)()

        if (isSuccess(req, resp)) {
          parseServiceResponse(resp.getResponseBody)
        } else {
          Left(new MojoExecutionException("Failed to upload file: " + file))
        }
      }
      case None => Left(new MojoExecutionException("Failed to identify package"))
    }
  }

  def installPackage(packageId: Option[PackageId]): Either[Throwable, (Boolean, String)] = {
    packageId match {
      case Some(id) => {
        val pkgPath = id.getInstallationPath + ".zip"
        val req = urlForPath(servicePath + pkgPath) << Map(
          "cmd" -> "install",
          "recursive" -> recursive.toString,
          "autosave" -> (autosave max 1024).toString
        )
        val resp = Http(req)()

        if (isSuccess(req, resp)) {
          parseServiceResponse(resp.getResponseBody)
        } else {
          Left(new MojoExecutionException("Failed to install package at path: " + pkgPath))
        }
      }
      case None => Left(new MojoExecutionException("Failed to identify package"))
    }
  }

  def existsOnServer(packageId: Option[PackageId]): Either[Throwable, (Boolean, String)] = {
    packageId match {
      case Some(id) => {
        val pkgPath = id.getInstallationPath + ".zip"
        val req = urlForPath(servicePath + pkgPath) << Map("cmd" -> "contents")
        val resp = Http(req)()

        if (isSuccess(req, resp)) {
          parseServiceResponse(resp.getResponseBody)
        } else {
          Left(new MojoExecutionException("failed to check for existence of package on server: " + pkgPath))
        }
      }
      case None => Left(new MojoExecutionException("Failed to identify package"))
    }
  }

  def parseServiceResponse(respBody: String): Either[Throwable, (Boolean, String)] = {
    import VltpackUtil._
    val json = List(JSON.parseFull(respBody))
    (for {
      Some(M(map)) <- json
      B(success) = map("success")
      S(msg) = map("msg")
    } yield (success, msg)).toList match {
      case head :: tail => Right(head)
      case _ => Left(new MojoExecutionException("failed to parse json response: " + respBody))
    }
  }
}