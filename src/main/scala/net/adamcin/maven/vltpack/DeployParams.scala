package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.Parameter

/**
 *
 * @version $Id: DeployParams.java$
 * @author madamcin
 */
trait DeployParams {

  @Parameter(property = "vlt.host", defaultValue = "localhost")
  var host: String = null

  @Parameter(property = "vlt.port", defaultValue = "4502")
  var port: Int = -1

  @Parameter(property = "vlt.user", defaultValue = "admin")
  var user: String = null

  @Parameter(property = "vlt.pass", defaultValue = "admin")
  var pass: String = null

  @Parameter(property = "vlt.deploy")
  var deploy = false

  @Parameter(property = "vlt.https")
  var https = false

  @Parameter(property = "vlt.proxy.noProxy")
  var noProxy = false

  @Parameter(property = "vlt.proxy.set")
  var proxySet = false

  @Parameter(property = "vlt.proxy.host", defaultValue = "localhost")
  var proxyHost: String = null

  @Parameter(property = "vlt.proxy.host", defaultValue = "-1")
  var proxyPort: Int = -1

  @Parameter(property = "vlt.proxy.user")
  var proxyUser: String = null

  @Parameter(property = "vlt.proxy.pass")
  var proxyPass: String = null

}