package net.adamcin.maven.vltpack

import com.ning.http.client.{RequestBuilder, Response}
import dispatch._
import org.apache.maven.plugins.annotations.Parameter
import java.io.File
import org.apache.maven.plugin.MojoExecutionException
import org.slf4j.LoggerFactory

/**
 *
 * @version $Id: PutsBundle.java$
 * @author madamcin
 */
trait PutsBundle extends HttpParameters with BundlePathParameters {
  val log = LoggerFactory.getLogger(getClass)

  @Parameter(property = "vlt.skip.mkdirs")
  val skipMkdirs = false

  /**
   * Puts the specified file to the configured location
   * @param file file to put
   * @return either log messages or a throwable
   */
  def putBundle(file: File): Either[List[String], Throwable] = {
    lazy val (putReq, putResp) = {
      val req = urlForPath(getBundleRepoPath(file.getName)) <<< file
      (req, Http(req)())
    }

    val fromMkdirs: Either[List[String], Throwable] = if (!skipMkdirs) {
      val (mkReq, mkResp) = mkdirs(bundleInstallPath)
      if (isSuccess(mkReq, mkResp)) {
        Left(List("successfully created path at " + mkReq.url))
      } else {
        log.debug("[putBundle] {}", getReqRespLogMessage(mkReq, mkResp))
        Right(new MojoExecutionException("failed to create path at: " + mkReq.url))
      }
    } else {
      Left(List("skipping mkdirs"))
    }

    fromMkdirs match {
      case Left(messages) => {
        if (file == null || !file.exists || !file.canRead) {
          Right(new MojoExecutionException("A valid file must be specified"))
        }
        if (isSuccess(putReq, putResp)) {
          Left(messages ++ List("successfully uploaded " + file + " to " + putReq.url))
        } else {
          log.debug("[putBundle] {}", getReqRespLogMessage(putReq, putResp))
          Right(new MojoExecutionException("failed to upload " + file + " to " + putReq.url))
        }
      }
      case _ => fromMkdirs
    }
  }

  def mkdirs(absPath: String): (RequestBuilder, Response) = {
    val segments = absPath.split('/').filter(!_.isEmpty)

    val dirs = segments.foldLeft(List.empty[String]) {
      (dirs: List[String], segment: String) => dirs match {
        case Nil => List(segment)
        case head :: tail => (head + "/" + segment) :: dirs
      }
    }.reverse

    dirs.foldLeft (null: (RequestBuilder, Response)) {
      (p: (RequestBuilder, Response), path: String) => {
        val doMkdir = Option(p) match {
          case Some((req, resp)) => isSuccess(req, resp)
          case None => true
        }
        if (doMkdir) { mkdir(path) } else { p }
      }
    }
  }

  def mkdir(absPath: String): (RequestBuilder, Response) = {
    val req = urlForPath(absPath).MKCOL
    (req, Http(req)())
  }
}