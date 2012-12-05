package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations._


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

  @Parameter(defaultValue = "true")
  val force = true

  @Parameter(defaultValue = "true")
  val recursive = true

  @Parameter(defaultValue = "1024")
  val autosave = 1024

  override def execute() {
    super.execute()

    if (!deploy || skip || project.getPackaging != "vltpack") {

      getLog.info("skipping [deploy=" + deploy + "][skip=" + skip + "][packaging=" + project.getPackaging + "]")

    } else {

      uploadPackage(project.getArtifact.getFile, force) match {
        case Left(messages) => messages.foreach { getLog.info(_) }
        case Right(t) => throw t
      }

      installPackage(project.getArtifact.getFile, recursive, autosave) match {
        case Left(messages) => messages.foreach { getLog.info(_) }
        case Right(t) => throw t
      }
    }

  }


}