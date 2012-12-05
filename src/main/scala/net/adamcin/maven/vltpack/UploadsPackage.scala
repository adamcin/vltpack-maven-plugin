package net.adamcin.maven.vltpack

import java.io.File
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Parameter
import dispatch._
import org.slf4j.LoggerFactory
import util.parsing.json.JSON
import net.adamcin.maven.vltpack.JSONUtil._
import com.ning.http.multipart.FilePart

/**
 *
 * @version $Id: UploadsPackage.java$
 * @author madamcin
 */
trait UploadsPackage extends BaseMojo with HttpParameters with IdentifiesPackages {
  val log = LoggerFactory.getLogger(getClass)

  lazy val servicePath = "/crx/packmgr/service/exec.json"

  def uploadPackage(file: File, force: Boolean): Either[List[String], Throwable] = {
    identifyPackage(file) match {
      case Some(id) => {
        val req = urlForPath(servicePath + id.getInstallationPath).POST <<? Map(
          "cmd" -> "upload",
          "force" -> force.toString
        )
        req.addBodyPart(new FilePart("package", id.getDownloadName, file))
        val resp = Http(req)()

        parseServiceResponse(resp.getResponseBody) match {
          case Left((success, msg)) => Left(List("uploading " + file + " to " + id.getInstallationPath + ": " + msg))
          case Right(t) => Right(t)
        }
      }
      case None => Right(new MojoExecutionException("Failed to identify package"))
    }
  }

  def installPackage(file: File, recursive: Boolean, autosave: Int): Either[List[String], Throwable] = {
    identifyPackage(file) match {
      case Some(id) => {
        val req = urlForPath(servicePath + id.getInstallationPath + ".zip") << Map(
          "cmd" -> "install",
          "recursive" -> recursive.toString,
          "autosave" -> (autosave max 1024).toString
        )
        val resp = Http(req)()

        parseServiceResponse(resp.getResponseBody) match {
          case Left((success, msg)) => Left(List("installing " + id.getInstallationPath + ": " + msg))
          case Right(t) => Right(t)
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

  def existsOnServer(file: File): Either[Boolean,Throwable] = {
    identifyPackage(file) match {
      case Some(id) => {
        val req = urlForPath(servicePath + id.getInstallationPath) << Map("cmd" -> "contents")
        val resp = Http(req)()

        if (isSuccess(req, resp)) {
          parseServiceResponse(resp.getResponseBody) match {
            case Left((success, msg)) => {
              getLog.info("checking server for existing package at " + id.getInstallationPath + ": " + msg)
              Left(success)
            }
            case Right(t) => Right(t)
          }
        } else {
          Right(new MojoExecutionException("failed to check for existence of package on server: " + id.getInstallationPath))
        }
      }
      case None => Right(new MojoExecutionException("Failed to identify package"))
    }
  }
}