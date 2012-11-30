package net.adamcin.maven.vltpack

import java.io.File
import org.apache.maven.plugin.MojoExecutionException
import com.day.jcr.vault.vlt.meta.xml.zip.ZipMetaDir
import com.day.jcr.vault.vlt.meta.xml.file.FileMetaDir
import com.day.jcr.vault.vlt.meta.MetaDirectory
import org.apache.maven.plugins.annotations.Parameter


/**
 *
 * @version $Id: CreatesPackage.java$
 * @author madamcin
 */
trait CreatesPackage extends LogsParameters {

  def createPackage(jcrRoot: File, metaInfRoot: File, outFile: File): Either[List[String], Throwable] = {
    val vltFile = new File(jcrRoot, ".vlt")
    if (!vltFile.exists || !vltFile.canRead) {
      Right(new MojoExecutionException(" Failed to read .vlt file"))
    } else {
      val metaDir: MetaDirectory = if (vltFile.isDirectory) {
        new ZipMetaDir(vltFile)
      } else {
        new FileMetaDir(vltFile)
      }

      val vltEntries = metaDir.getEntries


      Left(List("createPackage"))
    }
  }
}