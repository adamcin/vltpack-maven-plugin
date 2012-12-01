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

  /**
   * directory containing vltpack-generated files
   */
  lazy val vltpackDirectory = new File(outputDirectory, "vltpack")

  /**
   * directory containing resolved bundles
   */
  lazy val embedBundlesDirectory = new File(vltpackDirectory, "embed-bundles")

  /**
   * directory containing resolved packages
   */
  lazy val embedPackagesDirectory = new File(vltpackDirectory, "embed-packages")

  /**
   * vault-inf-generated META-INF/vault/... resources
   */
  lazy val vaultInfDirectory = new File(vltpackDirectory, "vault")
  lazy val configXml = new File(vaultInfDirectory, "config.xml")
  lazy val settingsXml = new File(vaultInfDirectory, "settings.xml")
  lazy val filterXml = new File(vaultInfDirectory, "filter.xml")
  lazy val propertiesXml = new File(vaultInfDirectory, "properties.xml")

}