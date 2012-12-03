package net.adamcin.maven.vltpack

import org.apache.maven.project.MavenProject
import org.apache.maven.plugins.annotations.{Parameter, LifecyclePhase, Mojo, Component}

import org.apache.maven.plugin.logging.Log

/**
 *
 * @version $Id: PutBundleMojo.java$
 * @author madamcin
 */
@Mojo(
  name = "put-bundle",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST,
  threadSafe = true)
class PutBundleMojo extends BaseMojo with PutsBundle with DeploysWithBuild {

  @Component
  var project: MavenProject = null

  @Parameter(property = "vlt.skip.put-bundle")
  val skip = false

  override def execute() {
    super.execute()

    if (!deploy || skip || project.getPackaging != "bundle") {
      getLog.info("skipping [deploy=" + deploy + "][skip=" + skip + "][packaging=" + project.getPackaging + "]")
    } else {
      putBundle(project.getArtifact.getFile) match {
        case Left(messages) => messages.foreach { getLog.info(_) }
        case Right(t) => throw t
      }
    }
  }

  override def printParams(log: Log) {
    super.printParams(log)
    getLog.info("vlt.skip.put-bundle = " + skip)
  }
}