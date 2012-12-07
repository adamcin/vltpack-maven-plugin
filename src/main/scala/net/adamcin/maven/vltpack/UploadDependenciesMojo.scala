package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.{LifecyclePhase, Parameter, Mojo}
import org.apache.maven.plugin.MojoExecutionException

/**
 *
 * @version $Id: UploadDependenciesMojo.java$
 * @author madamcin
 */
@Mojo(name = "upload-dependencies", defaultPhase = LifecyclePhase.INTEGRATION_TEST)
class UploadDependenciesMojo
  extends BaseMojo
  with DeploysWithBuild
  with RequiresProject
  with IdentifiesPackages
  with ResolvesArtifacts
  with UploadsPackage {

  @Parameter(property = "vlt.filter")
  val filter: String = null

  @Parameter(property = "vlt.skip.upload-dependencies")
  val skip = false

  @Parameter
  val force = false

  @Parameter(defaultValue = "true")
  val recursive = true

  @Parameter(defaultValue = "1024")
  val autosave = 1024

  override def execute() {
    super.execute()

    if (!deploy || skip || Option(filter).isEmpty || filter.length == 0) {

      getLog.info("skipping [deploy=" + deploy + "][skip=" + skip + "][filter=" + Option(filter).getOrElse("null") + "]")

    } else {
      resolveByFilter(filter).foreach {
        (artifact) => Option(artifact.getFile) match {
          case Some(file) => {
            val id = identifyPackage(file)
            val uploaded = uploadPackage(id, file, force) match {
              case Left((success, msg)) => {
                getLog.info("uploading " + file + " to " + id.get.getInstallationPath + ": " + msg)
                success
              }
              case Right(t) => throw t
            }

            if (uploaded) {
              installPackage(id, recursive, autosave) match {
                case Left((success, msg)) => {
                  getLog.info("installing " + id.get.getInstallationPath + ": " + msg)
                }
                case Right(t) => throw t
              }
            } else {
              getLog.info("package was not uploaded and so it will not be installed")
            }
          }
          case None => {
            throw new MojoExecutionException("failed to resolve artifact: " + artifact.getId)
          }
        }
      }
    }
  }
}