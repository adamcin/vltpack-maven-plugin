/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package net.adamcin.maven.vltpack.mojo

import org.apache.maven.plugin.MojoExecutionException
import net.adamcin.maven.vltpack._
import scala.Left
import scala.Some
import scala.Right
import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}

/**
 * Upload dependencies representing vault packages to the configured CQ server
 * @since 0.6.0
 * @author Mark Adamcin
 */
@Mojo(name = "upload-dependencies",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST,
  threadSafe = true)
class UploadDependenciesMojo
  extends BaseMojo
  with DeploysWithBuild
  with RequiresProject
  with PackageDependencies
  with IdentifiesPackages
  with ResolvesArtifacts
  with UploadsPackages {

  /**
   * Set to true to skip execution of this mojo
   * @since 1.0
   */
  @Parameter(property = "vltpack.skip.upload-dependencies")
  val skip = false

  /**
   * Force upload of packages if they already exist in the target environment
   * @since 1.0
   */
  @Parameter
  val force = false

  /**
   * Set to false to not install any subpackages that might be embedded within each dependency
   * @since 1.0
   */
  @Parameter(defaultValue = "true")
  val recursive = true

  /**
   * Change the autosave threshold for the install command
   * @since 1.0
   */
  @Parameter(defaultValue = "1024")
  val autosave = 1024

  override def execute() {
    super.execute()

    if (!deploy || skip || project.getPackaging != "vltpack") {

      getLog.info("skipping [deploy=" + deploy + "][skip=" + skip + "][packaging=" + project.getPackaging + "]")

    } else {
      packageDependencyArtifacts.foreach {
        (artifact) => Option(artifact.getFile) match {
          case None => throw new MojoExecutionException("failed to resolve artifact: " + artifact.getId)
          case Some(file) => {
            val id = identifyPackage(file)
            val doesntExist = force || (existsOnServer(id) match {
              case Right(t) => throw t
              case Left((success, msg)) => {
                getLog.info("checking for installed package " + id.get.getInstallationPath + ": " + msg)
                !success
              }
            })

            if (doesntExist) {
              val uploaded = uploadPackage(id, file, force) match {
                case Right(t) => throw t
                case Left((success, msg)) => {
                  getLog.info("uploading " + file + " to " + id.get.getInstallationPath + ".zip: " + msg)
                  success
                }
              }

              if (uploaded) {
                installPackage(id, recursive, autosave) match {
                  case Right(t) => throw t
                  case Left((success, msg)) => {
                    getLog.info("installing " + id.get.getInstallationPath + ".zip: " + msg)
                  }
                }
              } else {
                getLog.info("package was not uploaded and so it will not be installed")
              }
            }
          }
        }
      }
    }
  }
}