package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.Parameter
import java.io.File
import org.apache.maven.plugin.logging.Log

/**
 *
 * @version $Id: BundlePathParameters.java$
 * @author madamcin
 */
trait BundlePathParameters extends LogsParameters {

  final val defaultBundleInstallPath = "/apps/bundles/install/30"

  @Parameter(defaultValue = defaultBundleInstallPath)
  val bundleInstallPath: String = defaultBundleInstallPath

  @Parameter(property = "vlt.bundle.name")
  val bundleNameOverride: String = null

  def getBundleRepoPath(filename: String): String = {
    bundleInstallPath + "/" + filename
  }

  def getBundleName(file: File): String = {
    Option(bundleNameOverride) match {
      case Some(name) => bundleNameOverride
      case None => file.getName
    }
  }

  def getBundlePath(file: File): String = getBundleRepoPath(getBundleName(file))

  override def printParams(log: Log) {
    super.printParams(log)
    log.info("bundleInstallPath = " + bundleInstallPath)
    log.info("vlt.bundle.name = " + bundleNameOverride)
  }

}