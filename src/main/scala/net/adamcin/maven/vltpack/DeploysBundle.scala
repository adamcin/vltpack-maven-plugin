package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.artifact.Artifact
import org.apache.maven.plugin.AbstractMojo

/**
 *
 * @version $Id: DeploysBundle.java$
 * @author madamcin
 */
trait DeploysBundle extends AbstractMojo {

  final val defaultBundleInstallPath = "/apps/bundles/install/30"

  @Parameter(defaultValue = defaultBundleInstallPath)
  val bundleInstallPath: String = defaultBundleInstallPath

  def getBundleRepoPath(artifact: Artifact): String = {
    getBundleRepoPath(artifact.getArtifactId + "-" + artifact.getVersion + ".jar")
  }

  def getBundleRepoPath(filename: String): String = {
    bundleInstallPath + "/" + filename
  }
}