package net.adamcin.maven.vltpack

import java.io.File
import org.slf4j.LoggerFactory

/**
 *
 * @version $Id: OutputParameters.java$
 * @author madamcin
 */
trait OutputParameters extends RequiresProject {
  private val log = LoggerFactory.getLogger(getClass)

  /**
   * target directory
   */
  lazy val outputDirectory = getExistingDir(new File(project.getBuild.getDirectory))

  /**
   * directory containing vltpack-generated files
   */
  lazy val vltpackDirectory = getExistingDir(new File(outputDirectory, "vltpack"))

  /**
   * directory containing resolved bundles
   */
  lazy val embedBundlesDirectory = getExistingDir(new File(vltpackDirectory, "embed-bundles"))

  /**
   * directory containing resolved packages
   */
  lazy val embedPackagesDirectory = getExistingDir(new File(vltpackDirectory, "embed-packages"))

  /**
   * vault-inf-generated META-INF/vault/... resources
   */
  lazy val vaultInfDirectory = getExistingDir(new File(vltpackDirectory, "vault"))
  lazy val configXml = new File(vaultInfDirectory, "config.xml")
  lazy val settingsXml = new File(vaultInfDirectory, "settings.xml")
  lazy val filterXml = new File(vaultInfDirectory, "filter.xml")
  lazy val propertiesXml = new File(vaultInfDirectory, "properties.xml")

  def getExistingDir(file: File): File = {
    if (!file.exists() && !file.mkdir()) {
      log.error("[getExistingDir] failed to create directory: {}", file)
    }
    file
  }
}