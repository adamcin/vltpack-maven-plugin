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

package net.adamcin.maven.vltpack

import dispatch._
import com.ning.http.client.{ProxyServer, RequestBuilder, Response}
import org.apache.maven.plugins.annotations.Parameter

/**
 * Adds fluid support for the MKCOL method
 * @since 1.0
 * @author Mark Adamcin
 */
trait DavVerbs extends MethodVerbs {
  def MKCOL = subject.setMethod("MKCOL")
}

/**
 * Wraps an implicitly created DefaultRequestVerbs object with the DavVerbs trait
 * @param wrapped the requestbuilder wrapper to unwrap
 */
class DavRequestVerbs(wrapped: DefaultRequestVerbs) extends DefaultRequestVerbs(wrapped.subject) with DavVerbs

/**
 * Trait defining common mojo parameters and methods for establishing HTTP connections to a CQ server. Reuses the
 * vltpack.user parameter defined in the UsernameAware trait as part of the connection credentials
 * @since 1.0
 * @author Mark Adamcin
 */
trait HttpParameters extends UsernameAware {

  final val DEFAULT_HOST = "localhost"
  final val DEFAULT_PORT = "4502"
  final val DEFAULT_PASS = "admin"
  final val DEFAULT_CONTEXT = "/"
  final val DEFAULT_PROXY_PROTOCOL = "http"
  final val DEFAULT_PROXY_HOST = "localhost"

  /**
   * Hostname for the CQ server
   * @since 1.0
   */
  @Parameter(property = "vltpack.host", defaultValue = DEFAULT_HOST)
  val host = DEFAULT_HOST

  /**
   * TCP Port for the CQ server
   * @since 1.0
   */
  @Parameter(property = "vltpack.port", defaultValue = DEFAULT_PORT)
  val port = Integer.valueOf(DEFAULT_PORT)

  /**
   * Password to use in connection credentials
   * @since 1.0
   */
  @Parameter(property = "vltpack.pass", defaultValue = DEFAULT_PASS)
  val pass = DEFAULT_PASS

  /**
   * CQ Servlet context by which JCR paths are appended
   * @since 1.0
   */
  @Parameter(property = "vltpack.context", defaultValue = DEFAULT_CONTEXT)
  val context: String = DEFAULT_CONTEXT

  /**
   * Whether to use an SSL connection instead of a normal HTTP connection.
   * Does not affect the configured TCP port
   * @since 1.0
   */
  @Parameter(property = "vltpack.https")
  val https = false

  /**
   * Set to true to completely disable HTTP proxy connections for this plugin.
   * Overrides any other HTTP proxy configuration
   * @since 1.0
   */
  @Parameter(property = "vltpack.proxy.noProxy")
  val noProxy = false

  /**
   * Set to true to override the proxy configuration in the user's Maven Settings with the
   * associated mojo parameter alternatives
   * @since 1.0
   */
  @Parameter(property = "vltpack.proxy.set")
  val proxySet = false

  /**
   * The HTTP Proxy protocol
   * @since 1.0
   */
  @Parameter(property = "vltpack.proxy.protocol", defaultValue = DEFAULT_PROXY_PROTOCOL)
  val proxyProtocol: String = DEFAULT_PROXY_PROTOCOL

  /**
   * The HTTP Proxy hostname
   * @since 1.0
   */
  @Parameter(property = "vltpack.proxy.host", defaultValue = DEFAULT_PROXY_HOST)
  val proxyHost: String = DEFAULT_PROXY_HOST

  /**
   * The HTTP Proxy port. Set to -1 to use the default proxy port.
   * @since 1.0
   */
  @Parameter(property = "vltpack.proxy.port")
  val proxyPort: Int = -1

  /**
   * The HTTP Proxy username
   * @since 1.0
   */
  @Parameter(property = "vltpack.proxy.user")
  val proxyUser: String = null

  /**
   * The HTTP Proxy password
   * @since 1.0
   */
  @Parameter(property = "vltpack.proxy.pass")
  val proxyPass: String = null

  implicit def implyDavRequestVerbs(wrapped: DefaultRequestVerbs) = new DavRequestVerbs(wrapped)

  def urlForPath(absPath: String): DefaultRequestVerbs = {
    val segments = context.split('/') ++ absPath.split('/')
    segments.foldLeft(reqHost) { (req, segment) => req / segment }
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