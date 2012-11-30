package net.adamcin.maven.vltpack

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugin.logging.Log

/**
 *
 * @version $Id: BaseMojo.java$
 * @author madamcin
 */
abstract class BaseMojo extends AbstractMojo with LogsParameters {

  @Parameter(property = "vlt.debug")
  val debug = false

  def execute() {
    if (debug) {
      printParams(getLog)
    }
  }

  override def printParams(log: Log) {
    super.printParams(log)
    log.info("vlt.debug = " + debug)
  }
}