package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.Parameter

/**
 *
 * @version $Id: RepositoryPathParams.java$
 * @author madamcin
 */
trait RepositoryPathParams {

  @Parameter(property = "vlt.path.bundles", defaultValue = "/apps/bundles/install/30")
  var bundleInstallPath: String = null

  @Parameter(property = "vlt.path.crx", defaultValue = "/crx")
  var crxPath: String = null

  @Parameter(property = "vlt.path.sling", defaultValue = "/")
  var slingPath: String = null

}