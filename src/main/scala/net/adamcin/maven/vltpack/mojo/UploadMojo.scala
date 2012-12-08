package net.adamcin.maven.vltpack.mojo

import org.apache.maven.plugins.annotations._
import net.adamcin.maven.vltpack.{DeploysWithBuild, IdentifiesPackages, UploadsPackage}


/**
 *
 * @version $Id: UploadMojo.java$
 * @author madamcin
 */

@Mojo(
  name = "upload",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST)
class UploadMojo
  extends VltpackLifecycleMojo
  with UploadsPackage
  with IdentifiesPackages
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

      val file = targetFile
      val id = identifyPackage(file)

      val uploaded = uploadPackage(id, file, force) match {
        case Left((success, msg)) => {
          getLog.info("uploading " + file + " to " + id.get.getInstallationPath + ".zip: " + msg)
          success
        }
        case Right(t) => throw t
      }

      if (uploaded) {
        installPackage(id, recursive, autosave) match {
          case Left((success, msg)) => {
            getLog.info("installing " + id.get.getInstallationPath + ".zip: " + msg)
          }
          case Right(t) => throw t
        }
      } else {
        getLog.info("package was not uploaded and so it will not be installed")
      }
    }
  }
}