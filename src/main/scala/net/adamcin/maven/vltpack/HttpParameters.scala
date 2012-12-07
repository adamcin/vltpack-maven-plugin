package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.Parameter
import dispatch._
import com.ning.http.client.{RequestBuilder, Response}
import org.apache.maven.plugin.logging.Log

trait DavVerbs extends MethodVerbs {
  def MKCOL = subject.setMethod("MKCOL")
}

class DavRequestVerbs(wrapped: DefaultRequestVerbs) extends DefaultRequestVerbs(wrapped.subject) with DavVerbs

/**
 *
 * @version $Id: HttpParameters.scala$
 * @author madamcin
 */
trait HttpParameters extends UsernameAware {

  final val defaultHost = "localhost"
  final val defaultPort = "4502"
  final val defaultPass = "admin"
  final val defaultContext = "/"
  final val defaultProxyHost = "localhost"

  @Parameter(property = "vlt.host", defaultValue = defaultHost)
  val host = defaultHost

  @Parameter(property = "vlt.port", defaultValue = defaultPort)
  val port = Integer.valueOf(defaultPort)

  @Parameter(property = "vlt.pass", defaultValue = defaultPass)
  val pass = defaultPass

  @Parameter(property = "vlt.context", defaultValue = defaultContext)
  val context: String = defaultContext

  @Parameter(property = "vlt.https")
  val https = false

  @Parameter(property = "vlt.proxy.noProxy")
  val noProxy = false

  @Parameter(property = "vlt.proxy.set")
  val proxySet = false

  @Parameter(property = "vlt.proxy.host", defaultValue = defaultProxyHost)
  val proxyHost: String = defaultProxyHost

  @Parameter(property = "vlt.proxy.port")
  val proxyPort: Int = -1

  @Parameter(property = "vlt.proxy.user")
  val proxyUser: String = null

  @Parameter(property = "vlt.proxy.pass")
  val proxyPass: String = null

  override def printParams(log: Log) {
    super.printParams(log)
    log.info("vlt.host = " + host)
    log.info("vlt.port = " + port)
    log.info("vlt.pass = " + pass)
    log.info("vlt.context = " + context)
    log.info("vlt.https = " + https)
    log.info("vlt.proxy.noProxy = " + noProxy)
    log.info("vlt.proxy.set = " + proxySet)
    log.info("vlt.proxy.host = " + proxyHost)
    log.info("vlt.proxy.port = " + proxyPort)
    log.info("vlt.proxy.user = " + proxyUser)
    log.info("vlt.proxy.pass = " + proxyPass)
  }

  implicit def implyDavRequestVerbs(wrapped: DefaultRequestVerbs) = new DavRequestVerbs(wrapped)

  def urlForPath(absPath: String): DefaultRequestVerbs = {
    val segments = context.split('/') ++ absPath.split('/')
    segments.foldLeft(reqHost) { (req, segment) => req / segment }
  }

  def reqHost = List(dispatch.host(host, port)).map {
    (req) => if (https) { req.secure } else { req }
  }.map {
    // TODO: set proxy server
    (req) => req
  }.head as_!(user, pass)

  def isSuccess(req: RequestBuilder, resp: Response): Boolean = {
    (req.build().getMethod, Option(resp)) match {
      case ("MKCOL", Some(response)) => {
        Set(201, 405) contains response.getStatusCode
      }
      case ("PUT", Some(response)) => {
        Set(201, 204) contains response.getStatusCode
      }
      case (_, Some(response)) => {
        Set(200) contains response.getStatusCode
      }
      case _ => false
    }
  }

  def getReqRespLogMessage(req: RequestBuilder, resp: Response): String = {
    (Option(req), Option(resp)) match {
      case (Some(request), Some(response)) =>
        request.build().getMethod + " " + request.url + " => " + resp.getStatusCode + " " + resp.getStatusText
      case (Some(request), None) =>
        request.build().getMethod + " " + request.url + " => null"
      case (None, Some(response)) =>
        "null => " + resp.getStatusCode + " " + resp.getStatusText
      case _ => "null => null"
    }
  }

}