package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.Parameter
import com.ning.http.client.RequestBuilder
import org.apache.maven.plugin.AbstractMojo

/**
 *
 * @version $Id: UploadParameters.java$
 * @author madamcin
 */
trait UploadParameters extends AbstractMojo {

  final val defaultHost = "localhost"
  final val defaultPort = "4502"
  final val defaultUser = "admin"
  final val defaultPass = "admin"

  @Parameter(property = "vlt.host", defaultValue = defaultHost)
  val host = defaultHost

  @Parameter(property = "vlt.port", defaultValue = defaultPort)
  val port = Integer.valueOf(defaultPort)

  @Parameter(property = "vlt.user", defaultValue = defaultUser)
  val user = defaultUser

  @Parameter(property = "vlt.pass", defaultValue = defaultPass)
  val pass = defaultPass

  @Parameter(property = "vlt.deploy")
  var deploy = false

  @Parameter(property = "vlt.context", defaultValue = "/")
  var context: String = null

  @Parameter(property = "vlt.https")
  var https = false

  @Parameter(property = "vlt.proxy.noProxy")
  var noProxy = false

  @Parameter(property = "vlt.proxy.set")
  var proxySet = false

  @Parameter(property = "vlt.proxy.host", defaultValue = "localhost")
  var proxyHost: String = null

  @Parameter(property = "vlt.proxy.port")
  var proxyPort: Int = -1

  @Parameter(property = "vlt.proxy.user")
  var proxyUser: String = null

  @Parameter(property = "vlt.proxy.pass")
  var proxyPass: String = null

  def urlForPath(ctx: String, absPath: String) = {
    /*
    val plusContext = ctx match {
      case '/' :: more => ctx + absPath.tail
      case _ => absPath
    }
    host(host, port)
      */
  }

  def printParams() {
    getLog.info("vlt.host = " + host)
    getLog.info("vlt.port = " + port)
    getLog.info("vlt.user = " + user)
    getLog.info("vlt.pass = " + pass)
  }
}