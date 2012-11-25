package net.adamcin.maven.vltpack

import org.apache.maven.plugin.{MojoExecutionException, AbstractMojo}
import org.apache.maven.project.MavenProject
import org.apache.maven.plugins.annotations.{Parameter, LifecyclePhase, Mojo, Component}

import dispatch._

/**
 *
 * @version $Id: PutBundleMojo.java$
 * @author madamcin
 */
@Mojo(
  name = "put-bundle",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST,
  requiresProject = false, // TODO: remove this
  threadSafe = true)
class PutBundleMojo extends AbstractMojo with DeploysBundle with UploadParameters {

  @Component
  var project: MavenProject = null

  @Parameter(property = "vlt.skip.put-bundle")
  var skip = false

  def execute() {
    printParams()
    if (!deploy || skip) {
      getLog.info("[install-bundle] skipping [deploy=" + deploy + "][skip=" + skip + "]")
    } else if (project.getArtifact.getArtifactHandler.getExtension != "jar") {
      throw new MojoExecutionException("this goal can only be executed for *.jar artifacts")
    } else {

    }
  }


}