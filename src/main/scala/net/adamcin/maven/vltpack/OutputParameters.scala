package net.adamcin.maven.vltpack

import org.apache.maven.project.MavenProject
import org.apache.maven.plugins.annotations.Component
import java.io.File

/**
 *
 * @version $Id: OutputParameters.java$
 * @author madamcin
 */
trait OutputParameters extends RequiresProject {

  /**
   * target directory
   */
  lazy val outputDirectory = new File(project.getBuild.getDirectory)

  lazy val vltpackDirectory = new File(outputDirectory, "vltpack")

  lazy val embedBundlesDirectory = new File(vltpackDirectory, "embed-bundles")
  lazy val embedPackagesDirectory = new File(vltpackDirectory, "embed-packages")
}