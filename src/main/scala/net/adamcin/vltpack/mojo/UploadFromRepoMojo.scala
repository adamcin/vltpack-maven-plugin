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

import net.adamcin.vltpack.{IdentifiesPackages, ResolvesArtifacts, UploadsPackages}
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.{Mojo, Parameter}

/**
 * Command-line goal that provides the ability to resolve a package artifact from a local or remote repository and
 * install it directly into a running instance.
 * @since 1.0.0
 * @author Mark Adamcin
 */
@Mojo(name = "upload-from-repo",
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
   * Specify the full maven coordinates of the artifact in one of the following forms:
   *
   *  "groupId:artifactId:version"
   *  "groupId:artifactId:packaging:version"
   *  "groupId:artifactId:packaging:classifier:version"
   *
   *  where packaging is either "jar" or "zip" (the default is "jar")
   */
  @Parameter(property = "coords")
  val coords = ""

  override def execute() {
    super.execute()

    skipOrExecute(skip) {
      if (!coords.isEmpty) {
        resolveByCoordinates(coords) match {
          case Some(artifact) => uploadPackageArtifact(artifact)
          case None => throw new MojoExecutionException("Failed to resolve an artifact for coordinates " + coords)
        }
      } else {
        Option(proj) match {
          case Some(p) => {
            resolveArtifacts(Stream(p.getArtifact)) match {
              case head #:: tail => uploadPackageArtifact(head)
              case _ => throw new MojoExecutionException("Failed to resolve project artifact " + p.getArtifactId)
            }
          }
          case None => {
            throw new MojoExecutionException("The coords parameter is required when this goal is executed outside of a maven project")
          }
        }
      }
    }
  }
}
