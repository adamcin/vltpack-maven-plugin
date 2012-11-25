package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.artifact.Artifact

/**
 *
 * @version $Id: DeploysBundle.java$
 * @author madamcin
 */
trait DeploysBundle {

  @Parameter(defaultValue = "/apps/bundles/install/30")
  var bundleInstallPath: String = null

  def getBundleRepoPath(artifact: Artifact): String = {
    bundleInstallPath + "/" + artifact.getArtifactId + "-" + artifact.getVersion + ".jar"
  }
}