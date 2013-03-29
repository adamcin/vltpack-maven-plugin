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

import scalax.io.CloseAction
import java.io.{File, FilterOutputStream, OutputStream, InputStream}


/**
 * Utility methods used by the plugin
 * @since 0.6.0
 * @author Mark Adamcin
 */
object VltpackUtil {
  final val PACKAGING = "vltpack"

  val inputCloser: CloseAction[InputStream] = new CloseAction[InputStream] {
    protected def closeImpl(resource: InputStream) = {
      try {
        resource.close()
        Nil
      } catch {
        case t: Throwable => List(t)
      }
    }
  }

  val outputCloser: CloseAction[OutputStream] = new CloseAction[OutputStream] {
    protected def closeImpl(resource: OutputStream) = {
      try {
        resource.close()
        Nil
      } catch {
        case t: Throwable => List(t)
      }
    }
  }

  def blockClose(out: OutputStream): OutputStream = {
    new FilterOutputStream(out) {
      override def close() { /* do nothing */ }
    }
  }

  def noLeadingSlash(path: String) = Option(path) match {
    case Some(p) => if (p.startsWith("/")) p.substring(1, p.length) else p
    case None => ""
  }

  def noTrailingSlash(path: String) = Option(path) match {
    case Some(p) => if (p.endsWith("/")) p.substring(0, p.length - 1) else p
    case None => ""
  }

  def leadingSlashIfNotEmpty(path: String) = Option(path) match {
    case Some(p) => if (p.length > 0 && !p.startsWith("/")) "/" + p else p
    case None => ""
  }

  def toRelative(basedir: File, absolutePath: String) = {
    val rightSlashPath = absolutePath.replace('\\', '/')
    val basedirPath = basedir.getAbsolutePath.replace('\\', '/')
    if (rightSlashPath.startsWith(basedirPath)) {
      val fromBasePath = rightSlashPath.substring(basedirPath.length)
      val noSlash =
        if (fromBasePath.startsWith("/")) {
          fromBasePath.substring(1)
        } else {
          fromBasePath
        }

      if (noSlash.length() <= 0) {
        "."
      } else {
        noSlash
      }

    } else {
      rightSlashPath
    }
  }

  /**
   * Use these objects to match against a parsed json object, as in:
   *
   *  for {
   *    Some(M(map)) <- List(JSON.parseFull(jsonString))
   *    L(languages) = map("languages")
   *    M(language) <- languages
   *    S(name) = language("name")
   *    B(active) = language("is_active")
   *    D(completeness) = language("completeness")
   *  } yield {
   *    (name, active, completeness)
   *  }
   */
  class CC[T] { def unapply(a:Any):Option[T] = Some(a.asInstanceOf[T]) }

  object M extends CC[Map[String, Any]]
  object L extends CC[List[Any]]
  object S extends CC[String]
  object D extends CC[Double]
  object B extends CC[Boolean]
}