/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package net.adamcin.vltpack

import com.ning.http.client._
import dispatch._
import org.apache.maven.plugins.annotations.Parameter

import scala.annotation.tailrec
import scala.concurrent.ExecutionContext.Implicits

/**
 * Adds fluid support for the MKCOL method
 * @since 0.6.0
 * @author Mark Adamcin
 */
trait DavVerbs extends MethodVerbs {
  def MKCOL = subject.setMethod("MKCOL")
}

/**
 * Wraps an implicitly created DefaultRequestVerbs object with the DavVerbs trait
 * @param subject the requestbuilder wrapper to unwrap
 */
class DavRequestVerbs(val subject: dispatch.Req) extends MethodVerbs with DavVerbs

/**
 * Trait defining common mojo parameters and methods for establishing HTTP connections to a Granite server.
 * Reuses the vltpack.user parameter defined in the UsernameAware trait as part of the connection credentials
 * @since 0.6.0
 * @author Mark Adamcin
 */
trait HttpParameters extends UsernameAware {

  final val DEFAULT_HOST = "localhost"
  final val DEFAULT_PORT = "4502"
  final val DEFAULT_PASS = "admin"
  final val DEFAULT_CONTEXT = "/"
  final val DEFAULT_PROXY_PROTOCOL = "http"
  final val DEFAULT_PROXY_HOST = "localhost"

  final implicit val executor = Implicits.global

  /**
   * Hostname for the Granite server
   */
  @Parameter(property = "vltpack.host", defaultValue = DEFAULT_HOST)
  val host = DEFAULT_HOST

  /**
   * TCP Port for the Granite server
   */
  @Parameter(property = "vltpack.port", defaultValue = DEFAULT_PORT)
  val port = Integer.valueOf(DEFAULT_PORT)

  /**
   * Password to use in connection credentials
   */
  @Parameter(property = "vltpack.pass", defaultValue = DEFAULT_PASS)
  val pass = DEFAULT_PASS

  /**
   * Granite Servlet context by which JCR paths are appended
   */
  @Parameter(property = "vltpack.context", defaultValue = DEFAULT_CONTEXT)
  val context: String = DEFAULT_CONTEXT

  /**
   * Whether to use an SSL connection instead of a normal HTTP connection.
   * Does not affect the configured TCP port
   */
  @Parameter(property = "vltpack.https")
  val https = false

  /**
   * Set to true to completely disable HTTP proxy connections for this plugin.
   * Overrides any other HTTP proxy configuration
   */
  @Parameter(property = "vltpack.proxy.noProxy")
  val noProxy = false

  /**
   * Set to true to override the proxy configuration in the user's Maven Settings with the
   * associated mojo parameter alternatives
   */
  @Parameter(property = "vltpack.proxy.set")
  val proxySet = false

  /**
   * The HTTP Proxy protocol
   */
  @Parameter(property = "vltpack.proxy.protocol", defaultValue = DEFAULT_PROXY_PROTOCOL)
  val proxyProtocol: String = DEFAULT_PROXY_PROTOCOL

  /**
   * The HTTP Proxy hostname
   */
  @Parameter(property = "vltpack.proxy.host", defaultValue = DEFAULT_PROXY_HOST)
  val proxyHost: String = DEFAULT_PROXY_HOST

  /**
   * The HTTP Proxy port. Set to -1 to use the default proxy port.
   */
  @Parameter(property = "vltpack.proxy.port")
  val proxyPort: Int = -1

  /**
   * The HTTP Proxy username
   */
  @Parameter(property = "vltpack.proxy.user")
  val proxyUser: String = null

  /**
   * The HTTP Proxy password
   */
  @Parameter(property = "vltpack.proxy.pass")
  val proxyPass: String = null

  implicit def implyDavRequestVerbs(wrapped: Req) = new DavRequestVerbs(wrapped)

  def urlForPath(absPath: String): dispatch.Req = {
    val segments = context.split('/') ++ absPath.split('/')
    segments.foldLeft(reqHost) { (req, segment) => req / segment }
  }

  def baseUrlString: String = {
    val url = urlForPath("/").url
    url.substring(0, url.length - 1)
  }

  lazy val activeProxy: Option[ProxyServer] = {
    if (noProxy) {
      None
    } else if (proxySet) {
      val proxyServer =
        new ProxyServer(
          ProxyServer.Protocol.valueOf(Option(proxyProtocol).getOrElse("HTTP")),
          proxyHost,
          proxyPort,
          proxyUser,
          proxyPass)

      Some(proxyServer)
    } else {
      Option(settings.getActiveProxy) match {
        case None => None
        case Some(proxy) => {
          val proxyServer =
            new ProxyServer(
              ProxyServer.Protocol.valueOf(Option(proxy.getProtocol).getOrElse("HTTP")),
              proxy.getHost,
              proxy.getPort,
              proxy.getUsername,
              proxy.getPassword)

          Option(proxy.getNonProxyHosts) match {
            case None => ()
            case Some(nonProxyHosts) => {
              nonProxyHosts.split("\\|").foreach { proxyServer.addNonProxyHost(_) }
            }
          }

          Option(proxyServer)
        }
      }
      None
    }
  }

  def reqHost = List(dispatch.host(host, port)).map {
    (req) => if (https) { req.secure } else { req }
  }.map {
    (req) => activeProxy match {
      case None => req
      case Some(proxy) => req.setProxyServer(proxy)
    }
  }.head as_!(user, pass)

  def isSuccess(req: Req, resp: Response): Boolean = {
    (req.toRequest.getMethod, Option(resp)) match {
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

  def getReqRespLogMessage(req: Req, resp: Response): String = {
    (Option(req), Option(resp)) match {
      case (Some(request), Some(response)) =>
        request.toRequest.getMethod + " " + request.url + " => " + resp.getStatusCode + " " + resp.getStatusText
      case (Some(request), None) =>
        request.toRequest.getMethod + " " + request.url + " => null"
      case (None, Some(response)) =>
        "null => " + resp.getStatusCode + " " + resp.getStatusText
      case _ => "null => null"
    }
  }

  @tailrec
  final def waitForResponse[T](nTrys: Int)
                              (implicit until: Long,
                               requestFunction: () => (Request, AsyncHandler[T]),
                               contentChecker: (Future[T]) => Future[Boolean]): Boolean = {
    if (nTrys > 0) {
      val sleepTime = nTrys * 1000L
      getLog.info("sleeping " + nTrys + " seconds")
      Thread.sleep(sleepTime)
    }
    val mayProceed = contentChecker(for (res <- Http(requestFunction())) yield res)
    if (mayProceed()) {
      true
    } else {
      if (System.currentTimeMillis() >= until) {
        false
      } else {
        waitForResponse(nTrys + 1)
      }
    }
  }
}