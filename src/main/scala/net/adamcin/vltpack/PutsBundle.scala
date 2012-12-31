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

import com.ning.http.client.{RequestBuilder, Response}
import dispatch._
import java.io.File
import org.apache.maven.plugin.MojoExecutionException
import org.slf4j.LoggerFactory
import org.apache.maven.plugins.annotations.Parameter

/**
 * Trait defining common mojo parameters and methods for uploading OSGi bundles to the configured CQ server
 * using the PUT HTTP method
 * @since 0.6.0
 * @author Mark Adamcin
 */
trait PutsBundle extends HttpParameters with BundlePathParameters {
  val log = LoggerFactory.getLogger(getClass)

  /**
   * Set to true to skip the use of the MKCOL WebDAV method for the creation ancestor JCR paths
   */
  @Parameter(property = "vltpack.skip.mkdirs")
  var skipMkdirs = false

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