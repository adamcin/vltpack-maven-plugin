package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.{Mojo, LifecyclePhase}

/**
 *
 * @version $Id: VaultInfMojo.java$
 * @author madamcin
 */
@Mojo(
  name = "vault-inf",
  defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
class VaultInfMojo extends BaseMojo with OutputParameters {

}