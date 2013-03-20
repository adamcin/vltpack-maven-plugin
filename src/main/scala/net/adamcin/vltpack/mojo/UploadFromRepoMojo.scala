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

package net.adamcin.vltpack.mojo

import org.apache.maven.plugins.annotations.{Parameter, LifecyclePhase, Mojo}
import net.adamcin.vltpack.{IdentifiesPackages, ResolvesArtifacts, UploadsPackages}
import org.apache.maven.plugin.MojoExecutionException

/**
 * Embeds bundles in the project artifact at the configured bundleInstallPath
 * @since 1.0.0
 * @author Mark Adamcin
 */
@Mojo(name = "upload-from-repo",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST,
  requiresProject = false,
  threadSafe = true)
class UploadFromRepoMojo
  extends BaseMojo
  with ResolvesArtifacts
  with IdentifiesPackages
  with UploadsPackages {

  /**
   * Set to true to skip execution of this mojo
   */
  @Parameter(property = "vltpack.skip.upload-from-repo")
  val skip = false

  /**
   * Force upload of packages if they already exist in the target environment
   */
  @Parameter(property = "force")
  val force = false

  /**
   * Specify the full maven coordinates of the artifact in one of the following forms:
   *
   *  "groupId:artifactId:version"
   *  "groupId:artifactId:packaging:version"
   *  "groupId:artifactId:packaging:classifier:version"
   *
   *  where packaging is either "jar" or "zip" (the default is "jar")
   */
  @Parameter(property = "coords", required = true)
  val coords = ""

  override def execute() {
    super.execute()

    if (skip) {

      getLog.info("skipping [skip=" + skip + "]")

    } else {
      resolveByCoordinates(coords) foreach {
        (artifact) => Option(artifact.getFile) match {
          case None => throw new MojoExecutionException("failed to resolve artifact: " + artifact.getId)
          case Some(file) => {
            val id = identifyPackage(file)
            val doesntExist = force || (existsOnServer(id) match {
              case Left(t) => throw t
              case Right((success, msg)) => {
                val successMsg = if (success) "Package exists" else "Package not found"
                getLog.info("checking for installed package " + id.get.getInstallationPath + ".zip: " + successMsg)
                !success
              }
            })

            if (doesntExist) {
              val uploaded = uploadPackage(id, file, force) match {
                case Left(t) => throw t
                case Right((success, msg)) => {
                  getLog.info("uploading " + file + " to " + id.get.getInstallationPath + ".zip: " + msg)
                  success
                }
              }

              if (uploaded) {
                installPackage(id) match {
                  case Left(t) => throw t
                  case Right((success, msg)) => {
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
