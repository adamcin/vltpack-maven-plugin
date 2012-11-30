package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugin.logging.Log

/**
 *
 * @version $Id: DeploysWithBuild.java$
 * @author madamcin
 */
trait DeploysWithBuild extends LogsParameters {

  @Parameter(property = "vlt.deploy")
  val deploy = false

  override def printParams(log: Log) {
    super.printParams(log)
    log.info("vlt.deploy = " + deploy)
  }
}