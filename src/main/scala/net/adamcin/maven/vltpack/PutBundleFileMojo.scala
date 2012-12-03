package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}
import java.io.File
import org.apache.maven.plugin.logging.Log

/**
 *
 * @version $Id: PutBundleFileMojo.java$
 * @author madamcin
 */
@Mojo(
  name = "put-bundle-file",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST,
  requiresProject = false,
  threadSafe = true)
class PutBundleFileMojo extends BaseMojo with PutsBundle {

  @Parameter(property = "file", required = true)
  val file: File = null

  override def execute() {
    putBundle(file) match {
      case Left(messages) => messages.foreach { getLog.info(_) }
      case Right(t) => throw t
    }
  }

  override def printParams(log: Log) {
    super.printParams(log)
    getLog.info("file = " + file)
  }
}


