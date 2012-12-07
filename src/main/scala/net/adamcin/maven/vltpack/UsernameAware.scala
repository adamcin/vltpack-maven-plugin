package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugin.logging.Log

/**
 *
 * @version $Id: UsernameAware.java$
 * @author madamcin
 */
trait UsernameAware extends LogsParameters {

  final val defaultUser = "admin"

  @Parameter(property = "vlt.user", defaultValue = defaultUser)
  val user = defaultUser

  override def printParams(log: Log) {
    super.printParams(log)
    log.info("vlt.user = " + user)
  }
}