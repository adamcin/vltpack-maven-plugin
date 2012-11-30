package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations._
import org.apache.maven.project.MavenProject
import java.io.File


/**
 *
 * @version $Id: UploadMojo.java$
 * @author madamcin
 */

@Mojo(
  name = "upload",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST)
class UploadMojo
  extends BaseMojo
  with RequiresProject
  with UploadsPackage
  with DeploysWithBuild {

  @Parameter(property = "vlt.skip.upload")
  val skip = false

  override def execute() {
    super.execute()

    if (!deploy || skip || project.getPackaging != "vltpack") {
      getLog.info("[upload] skipping [deploy=" + deploy + "][skip=" + skip + "][packaging=" + project.getPackaging + "]")
    } else {
      uploadPackage(project.getArtifact.getFile) match {
        case Left(messages) => messages.foreach {
          (mesg) => getLog.info("[upload] " + mesg)
        }
        case Right(t) => throw t
      }
    }

  }


}