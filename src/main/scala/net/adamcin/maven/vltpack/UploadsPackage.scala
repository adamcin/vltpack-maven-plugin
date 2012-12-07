package net.adamcin.maven.vltpack

import java.io.File
import org.apache.maven.plugin.MojoExecutionException
import dispatch._
import org.slf4j.LoggerFactory
import util.parsing.json.JSON
import net.adamcin.maven.vltpack.JSONUtil._
import com.ning.http.multipart.FilePart
import com.day.jcr.vault.packaging.PackageId

/**
 *
 * @version $Id: UploadsPackage.java$
 * @author madamcin
 */
trait UploadsPackage extends HttpParameters {
  val log = LoggerFactory.getLogger(getClass)

  lazy val servicePath = "/crx/packmgr/service/exec.json"

  def uploadPackage(packageId: Option[PackageId], file: File, force: Boolean): Either[(Boolean, String), Throwable] = {
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
          Right(new MojoExecutionException("Failed to upload file: " + file))
        }
      }
      case None => Right(new MojoExecutionException("Failed to identify package"))
    }
  }

  def installPackage(packageId: Option[PackageId], recursive: Boolean, autosave: Int): Either[(Boolean, String), Throwable] = {
    packageId match {
      case Some(id) => {
        val req = urlForPath(servicePath + id.getInstallationPath + ".zip") << Map(
          "cmd" -> "install",
          "recursive" -> recursive.toString,
          "autosave" -> (autosave max 1024).toString
        )
        val resp = Http(req)()

        if (isSuccess(req, resp)) {
          parseServiceResponse(resp.getResponseBody)
        } else {
          Right(new MojoExecutionException("Failed to install package at path: " + id.getInstallationPath))
        }
      }
      case None => Right(new MojoExecutionException("Failed to identify package"))
    }
  }

  def existsOnServer(packageId: Option[PackageId]): Either[(Boolean, String),Throwable] = {
    packageId match {
      case Some(id) => {
        val req = urlForPath(servicePath + id.getInstallationPath) << Map("cmd" -> "contents")
        val resp = Http(req)()

        if (isSuccess(req, resp)) {
          parseServiceResponse(resp.getResponseBody)
        } else {
          Right(new MojoExecutionException("failed to check for existence of package on server: " + id.getInstallationPath))
        }
      }
      case None => Right(new MojoExecutionException("Failed to identify package"))
    }
  }

  def parseServiceResponse(respBody: String): Either[(Boolean, String),Throwable] = {
    val json = List(JSON.parseFull(respBody))
    (for {
      Some(M(map)) <- json
      B(success) = map("success")
      S(msg) = map("msg")
    } yield (success, msg)).toList match {
      case head :: tail => Left(head)
      case _ => Right(new MojoExecutionException("failed to parse json response: " + respBody))
    }
  }
}