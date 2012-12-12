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

import org.apache.maven.plugin.MojoExecutionException
import net.adamcin.maven.vltpack._
import scala.Left
import scala.Some
import scala.Right
import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}

/**
 * Upload dependencies representing vault packages to the configured CQ server
 * @since 1.0
 * @author Mark Adamcin
 */
@Mojo(name = "upload-dependencies",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST)
class UploadDependenciesMojo
  extends BaseMojo
  with DeploysWithBuild
  with RequiresProject
  with PackageDependencies
  with IdentifiesPackages
  with ResolvesArtifacts
  with UploadsPackage {

  /**
   * Set to true to skip execution of this mojo
   * @since 1.0
   */
  @Parameter(property = "vlt.skip.upload-dependencies")
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