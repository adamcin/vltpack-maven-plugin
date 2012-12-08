package net.adamcin.maven.vltpack.mojo

import collection.JavaConversions
import org.apache.maven.plugin.logging.Log
import java.util.Collections
import org.apache.maven.plugins.annotations._
import org.apache.maven.plugin.MojoExecutionException
import net.adamcin.maven.vltpack.{BundlePathParameters, VltpackUtil, OutputParameters, ResolvesArtifacts}
import java.io.File

/**
 *
 * @version $Id: EmbedBundlesMojo.java$
 * @author madamcin
 */

@Mojo(
  name = "embed-bundles",
  defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
class EmbedBundlesMojo
  extends BaseMojo
  with ResolvesArtifacts
  with BundlePathParameters
  with OutputParameters {

  @Parameter
  val embedBundles = Collections.emptyList[String]

  override def execute() {
    super.execute()

    val artifacts = resolveByArtifactIds(JavaConversions.collectionAsScalaIterable(embedBundles).toSet)
    val dir = new File(embedBundlesDirectory, VltpackUtil.noLeadingSlash(VltpackUtil.noTrailingSlash(bundleInstallPath)))
    if (dir.isDirectory || dir.mkdirs()) {
      artifacts.foreach( copyToDir(dir, getLog)_ )
    } else {
      throw new MojoExecutionException("Failed to create directory: " + dir)
    }
  }

  override def printParams(log: Log) {
    super.printParams(log)

    log.info("embedBundles:")
    JavaConversions.collectionAsScalaIterable(embedBundles).foreach {
      (embedBundle) => log.info("  " + embedBundle)
    }
  }
}