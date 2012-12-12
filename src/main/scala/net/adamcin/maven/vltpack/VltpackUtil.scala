/*
 * Copyright 2012 Mark Adamcin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adamcin.maven.vltpack

import scalax.io.CloseAction
import java.io.{File, FilterOutputStream, OutputStream, InputStream}


/**
 * Utility methods used by the plugin
 * @since 1.0
 * @author Mark Adamcin
 */
object VltpackUtil {

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