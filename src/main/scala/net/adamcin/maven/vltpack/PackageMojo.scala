package net.adamcin.maven.vltpack

import java.util.Collections
import org.apache.maven.plugins.annotations._
import org.apache.maven.project.MavenProject
import collection.JavaConversions
import org.apache.maven.artifact.Artifact
import java.io.File
import org.apache.maven.plugin.logging.Log

/**
 *
 * @version $Id: PackageMojo.java$
 * @author madamcin
 */
@Mojo(
  name = "package",
  defaultPhase = LifecyclePhase.PACKAGE)
class PackageMojo extends BaseMojo with OutputParameters {

  final val defaultJcrPath = "/"

  @Parameter(property = "vlt.jcrPath", defaultValue = defaultJcrPath)
  val jcrPath: String = defaultJcrPath

  @Parameter
  val embedBundles = Collections.emptyList[String]

  @Parameter
  val embedPackages = Collections.emptyList[String]

  override def printParams(log: Log) {
    super.printParams(log)

    log.info("embedBundles:")
    JavaConversions.collectionAsScalaIterable(embedBundles).foreach {
      (embedBundle) => log.info("  " + embedBundle)
    }

    log.info("embedPackages:")
    JavaConversions.collectionAsScalaIterable(embedPackages).foreach {
      (embedPackage) => log.info("  " + embedPackage)
    }
  }

  override def execute() {
    super.execute()
    val deps = JavaConversions.collectionAsScalaIterable(project.getDependencyArtifacts).
      filter { art: Artifact => art.getType == null || art.getType == "jar" }.
      map { art: Artifact => (art.getArtifactId, art) }.toMap


    val outputDirectory = new File(project.getBuild.getOutputDirectory)


  }



}