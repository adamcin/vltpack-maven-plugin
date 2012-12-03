package net.adamcin.maven.vltpack

import scalax.io.CloseAction
import java.io.{FilterOutputStream, OutputStream, InputStream}

/**
 *
 * @version $Id: IOUtil.java$
 * @author madamcin
 */
object IOUtil {

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

}