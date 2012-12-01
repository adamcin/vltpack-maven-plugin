package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.{Mojo, LifecyclePhase}
import org.apache.maven.plugin.logging.Log

/**
 *
 * @version $Id: VaultInfMojo.java$
 * @author madamcin
 */
@Mojo(
  name = "vault-inf",
  defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
class VaultInfMojo extends BaseMojo with OutputParameters {

  override def execute() {
    super.execute()
  }

  override def printParams(log: Log) {
    super.printParams(log)
  }
}