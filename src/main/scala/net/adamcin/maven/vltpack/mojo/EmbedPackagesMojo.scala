package net.adamcin.maven.vltpack.mojo

import collection.JavaConversions
import org.apache.maven.plugin.logging.Log
import java.util.Collections
import org.apache.maven.plugins.annotations.{Mojo, LifecyclePhase, Parameter}
import org.apache.maven.plugin.MojoExecutionException
import net.adamcin.maven.vltpack.{OutputParameters, ResolvesArtifacts}

/**
 *
 * @version $Id: EmbedPackagesMojo.java$
 * @author madamcin
 */

@Mojo(
  name = "embed-packages",
  defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
class EmbedPackagesMojo extends BaseMojo with ResolvesArtifacts with OutputParameters {

  @Parameter
  val embedPackages = Collections.emptyList[String]

  override def execute() {
    super.execute()

    val artifacts = resolveByArtifactIds(JavaConversions.collectionAsScalaIterable(embedPackages).toSet)

    if (embedPackagesDirectory.isDirectory || embedPackagesDirectory.mkdirs()) {
      artifacts.foreach( copyToDir(embedPackagesDirectory, getLog)_ )
    } else {
      throw new MojoExecutionException("Failed to create directory: " + embedPackagesDirectory)
    }
  }

  override def printParams(log: Log) {
    super.printParams(log)

    log.info("embedPackages:")
    JavaConversions.collectionAsScalaIterable(embedPackages).foreach {
      (embedPackage) => log.info("  " + embedPackage)
    }
  }
}