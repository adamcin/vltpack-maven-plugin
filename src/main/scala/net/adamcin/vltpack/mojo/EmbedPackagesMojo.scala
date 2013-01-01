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

import collection.JavaConversions
import java.util.Collections
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}
import net.adamcin.vltpack.{ResolvesArtifacts, OutputParameters}

/**
 * Embed sub packages in the project artifact package under /etc/packages
 * @since 0.6.0
 * @author Mark Adamcin
 */
@Mojo(name = "embed-packages",
  defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
  threadSafe = true)
class EmbedPackagesMojo
  extends BaseMojo
  with ResolvesArtifacts
  with OutputParameters {

  /**
   * List of artifactIds matching project dependencies that should be embedded
   */
  @Parameter
  val embedPackages = Collections.emptyList[String]

  override def execute() {
    super.execute()

    val artifacts = resolveByArtifactIds(JavaConversions.collectionAsScalaIterable(embedPackages).toSet)

    if (embedPackagesDirectory.isDirectory || embedPackagesDirectory.mkdirs()) {
      artifacts.foreach( copyToDir(embedPackagesDirectory, getLog)_ )
    } else {
      throw new MojoExecutionException("Failed to create directory: " + embedPackagesDirectory)
    }
  }
}