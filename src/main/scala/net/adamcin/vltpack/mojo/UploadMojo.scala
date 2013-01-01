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

import scala.Left
import scala.Right
import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}
import net.adamcin.vltpack.{UploadsPackages, OutputParameters, IdentifiesPackages, DeploysWithBuild}


/**
 * Uploads the project vault package to the configured CQ server
 * @since 0.6.0
 * @author Mark Adamcin
 */
@Mojo(name = "upload",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST,
  threadSafe = true)
class UploadMojo
  extends BaseMojo
  with OutputParameters
  with UploadsPackages
  with IdentifiesPackages
  with DeploysWithBuild {

  /**
   * Set to true to skip mojo execution
   */
  @Parameter(property = "vltpack.skip.upload")
  var skip = false

  /**
   * Force upload of the package if it already exists in the target environment
   */
  @Parameter(defaultValue = "true")
  var force = true

  /**
   * Set to false to not install any embedded subpackages
   */
  @Parameter(defaultValue = "true")
  var recursive = true

  /**
   * Change the autosave threshold for the install command
   */
  @Parameter(defaultValue = "1024")
  var autosave = 1024

  override def execute() {
    super.execute()

    if (!deploy || skip || project.getPackaging != "vltpack") {

      getLog.info("skipping [deploy=" + deploy + "][skip=" + skip + "][packaging=" + project.getPackaging + "]")

    } else {

      val file = targetFile
      val id = identifyPackage(file)

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