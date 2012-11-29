package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugin.AbstractMojo
import dispatch._
import com.ning.http.client.{HttpResponseStatus, Response, ProxyServer}

trait DavVerbs extends MethodVerbs {
  def MKCOL = subject.setMethod("MKCOL")
}

class DavRequestVerbs(wrapped: DefaultRequestVerbs) extends DefaultRequestVerbs(wrapped.subject) with DavVerbs

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
  final val defaultContext = "/"
  final val defaultProxyHost = "localhost"

  @Parameter(property = "vlt.host", defaultValue = defaultHost)
  val host = defaultHost

  @Parameter(property = "vlt.port", defaultValue = defaultPort)
  val port = Integer.valueOf(defaultPort)

  @Parameter(property = "vlt.user", defaultValue = defaultUser)
  val user = defaultUser

  @Parameter(property = "vlt.pass", defaultValue = defaultPass)
  val pass = defaultPass

  @Parameter(property = "vlt.deploy")
  val deploy = false

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

  def mkdirs(absPath: String): Response = {
    val segments = absPath.split('/').filter(!_.isEmpty)

    val dirs = segments.foldLeft(List.empty[String]) {
      (dirs: List[String], segment: String) => dirs match {
        case Nil => List(segment)
        case head :: tail => (head + "/" + segment) :: dirs
      }
    }.reverse

    dirs.foldLeft (null: Response) {
      (resp: Response, path: String) => {
        if (resp == null || resp.getStatusCode == 201 || resp.getStatusCode == 405) {
          mkdir(path)
        } else {
          resp
        }
      }
    }
  }

  def mkdir(absPath: String): Response = {
    //getLog.info("[mkdir] absPath=" + absPath)
    Http(urlForPath(absPath).MKCOL)()
  }

  def printParams() {
    getLog.info("vlt.host = " + host)
    getLog.info("vlt.port = " + port)
    getLog.info("vlt.user = " + user)
    getLog.info("vlt.pass = " + pass)
  }
}