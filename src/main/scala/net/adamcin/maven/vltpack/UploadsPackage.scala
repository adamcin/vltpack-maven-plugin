package net.adamcin.maven.vltpack

import java.io.File

/**
 *
 * @version $Id: UploadsPackage.java$
 * @author madamcin
 */
trait UploadsPackage extends HttpParameters with IdentifiesPackages {

  def uploadPackage(file: File): Either[List[String], Throwable] = {
    Left(List("uploadPackage(File)"))
  }

}