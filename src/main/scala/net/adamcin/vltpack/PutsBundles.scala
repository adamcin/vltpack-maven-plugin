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

import com.ning.http.client.Response
import dispatch._
import org.apache.jackrabbit.vault.util.Text
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Parameter
import org.slf4j.LoggerFactory

/**
 * Trait defining common mojo parameters and methods for uploading OSGi bundles to the configured CQ server
 * using the PUT HTTP method
 * @since 0.6.0
 * @author Mark Adamcin
 */
trait PutsBundles extends HttpParameters with BundlePathParameters {
  private val log = LoggerFactory.getLogger(getClass)

  /**
   * Set to true to skip the use of the MKCOL WebDAV method for the creation ancestor JCR paths
   */
  @Parameter(property = "vltpack.skip.mkdirs")
  var skipMkdirs = false

  /**
   * Puts the specified file to the configured bundle install location
   * @param file file to put
   * @return either log messages or a throwable
   */
  def putBundle(file: File): Either[Throwable, List[String]] = {
    putBundleToPath(file, getBundleRepoPath(file.getName))
  }

  /**
   * Puts the specified file to the configured test bundle install location
   * @param file file to put
   * @return either log messages or a throwable
   */
  def putTestBundle(file: File): Either[Throwable, List[String]] = {
    putBundleToPath(file, getTestBundleRepoPath(file.getName))
  }

  def putBundleToPath(file: File, path: String): Either[Throwable, List[String]] = {
    lazy val (putReq: Req, putResp: Response) = {
      val req = urlForPath(path) <<< file
      (req, Http(req).apply())
    }

    val fromMkdirs: Either[Throwable, List[String]] = if (!skipMkdirs) {
      val (mkReq, mkResp) = mkdirs(Text.getRelativeParent(path, 1))
      if (isSuccess(mkReq, mkResp)) {
        Right(List("successfully created path at " + mkReq.url))
      } else {
        log.debug("[putBundle] {}", getReqRespLogMessage(mkReq, mkResp))
        Left(new MojoExecutionException("failed to create path at: " + mkReq.url))
      }
    } else {
      Right(List("skipping mkdirs"))
    }

    fromMkdirs match {
      case Right(messages) => {
        if (file == null || !file.exists || !file.canRead) {
          Left(new MojoExecutionException("A valid file must be specified"))
        }
        if (isSuccess(putReq, putResp)) {
          Right(messages ++ List("successfully uploaded " + file + " to " + putReq.url))
        } else {
          log.debug("[putBundle] {}", getReqRespLogMessage(putReq, putResp))
          Left(new MojoExecutionException("failed to upload " + file + " to " + putReq.url))
        }
      }
      case _ => fromMkdirs
    }
  }

  def mkdirs(absPath: String): (Req, Response) = {
    val segments = absPath.split('/').filter(!_.isEmpty)

    val dirs = segments.foldLeft(List.empty[String]) {
      (dirs: List[String], segment: String) => dirs match {
        case Nil => List(segment)
        case head :: tail => (head + "/" + segment) :: dirs
      }
    }.reverse

    dirs.foldLeft (null: (Req, Response)) {
      (p: (Req, Response), path: String) => {
        val doMkdir = Option(p) match {
          case Some((req, resp)) => isSuccess(req, resp)
          case None => true
        }
        if (doMkdir) { mkdir(path) } else { p }
      }
    }
  }

  def mkdir(absPath: String): (Req, Response) = {
    val req = urlForPath(absPath).MKCOL
    (req, Http(req).apply())
  }
}