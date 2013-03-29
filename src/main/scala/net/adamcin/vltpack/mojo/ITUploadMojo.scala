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

import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}
import net.adamcin.vltpack.{PackageDependencies, UploadsPackages, OutputParameters}


/**
 * Uploads the project vault package and its dependencies to the configured IT server
 * @since 1.0.0
 * @author Mark Adamcin
 */
@Mojo(name = "IT-upload",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST,
  threadSafe = true)
class ITUploadMojo
  extends BaseITMojo
  with OutputParameters
  with UploadsPackages
  with PackageDependencies {

  /**
   * Set to true to skip mojo execution
   */
  @Parameter(property = "vltpack.skip.IT-upload")
  var skip = false

  /**
   * Force upload of the package if it already exists in the target environment
   */
  @Parameter(defaultValue = "true")
  var force = true

  /**
   * Force upload of the package dependencies if they already exist in the target environment
   */
  @Parameter(defaultValue = "false")
  var forceDependencies = false

  override def execute() {
    super.execute()

    skipOrExecute(skip) {
      getLog.info("uploading package dependencies...")
      packageDependencyArtifacts.foreach { uploadPackageArtifact(_, forceDependencies) }

      val file = targetFile
      val id = identifyPackage(file)

      val uploaded = uploadPackage(id, file, force) fold (throw _, (resp) => {
        val (success, msg) = resp
        getLog.info("uploading " + file + " to " + id.get.getInstallationPath + ".zip: " + msg)
        success
      })

      if (uploaded) {
        val installed = installPackage(id) fold (throw _, (resp) => {
          val (success, msg) = resp
          getLog.info("installing " + id.get.getInstallationPath + ".zip: " + msg)
          success
        })

        if (!installed) {
          getLog.info("package was not installed")
        }
      } else {
        getLog.info("package was not uploaded and so it will not be installed")
      }
    }
  }
}