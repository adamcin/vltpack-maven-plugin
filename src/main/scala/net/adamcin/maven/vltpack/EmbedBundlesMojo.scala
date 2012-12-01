package net.adamcin.maven.vltpack

import collection.JavaConversions
import org.apache.maven.plugin.logging.Log
import java.util.Collections
import org.apache.maven.plugins.annotations._
import java.io.File
import org.apache.maven.repository.RepositorySystem
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest
import org.apache.maven.project.MavenProject
import scalax.io.Resource
import org.apache.maven.plugin.MojoExecutionException

/**
 *
 * @version $Id: EmbedBundlesMojo.java$
 * @author madamcin
 */

@Mojo(
  name = "embed-bundles",
  defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
class EmbedBundlesMojo extends BaseMojo with ResolvesArtifacts with OutputParameters {

  @Parameter
  val embedBundles = Collections.emptyList[String]

  override def execute() {
    super.execute()

    val artifacts = resolveByArtifactIds(JavaConversions.collectionAsScalaIterable(embedBundles).toSet)

    if (embedBundlesDirectory.isDirectory || embedBundlesDirectory.mkdirs()) {
      artifacts.foreach( copyToDir(embedBundlesDirectory, getLog)_ )
    } else {
      throw new MojoExecutionException("Failed to create directory: " + embedBundlesDirectory)
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