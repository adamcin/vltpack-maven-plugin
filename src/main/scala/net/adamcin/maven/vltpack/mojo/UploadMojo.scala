/*
 * Copyright 2012 Mark Adamcin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adamcin.maven.vltpack.mojo

import net.adamcin.maven.vltpack.{OutputParameters, DeploysWithBuild, IdentifiesPackages, UploadsPackage}
import scala.Left
import scala.Right
import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}


/**
 * Uploads the project vault package to the configured CQ server
 * @since 1.0
 * @author Mark Adamcin
 */
@Mojo(name = "upload",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST)
class UploadMojo
  extends BaseMojo
  with OutputParameters
  with UploadsPackage
  with IdentifiesPackages
  with DeploysWithBuild {

  /**
   * Set to true to skip mojo execution
   * @since 1.0
   */
  @Parameter(property = "vlt.skip.upload")
  var skip = false

  /**
   * Force upload of the package if it already exists in the target environment
   * @since 1.0
   */
  @Parameter(defaultValue = "true")
  var force = true

  /**
   * Set to false to not install any embedded subpackages
   * @since 1.0
   */
  @Parameter(defaultValue = "true")
  var recursive = true

  /**
   * Change the autosave threshold for the install command
   * @since 1.0
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