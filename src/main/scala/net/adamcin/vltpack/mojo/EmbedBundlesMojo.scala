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
import java.io.File
import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}
import net.adamcin.vltpack.{VltpackUtil, ResolvesArtifacts, OutputParameters, BundlePathParameters}

/**
 * Embeds bundles in the project artifact at the configured bundleInstallPath
 * @since 0.6.0
 * @author Mark Adamcin
 */
@Mojo(name = "embed-bundles",
  defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
  threadSafe = true)
class EmbedBundlesMojo
  extends BaseMojo
  with ResolvesArtifacts
  with BundlePathParameters
  with OutputParameters {

  /**
   * List of articleIds matching dependencies that should be embedded
   */
  @Parameter
  val embedBundles = Collections.emptyList[String]

  override def execute() {
    super.execute()

    val artifacts = resolveByArtifactIds(JavaConversions.collectionAsScalaIterable(embedBundles).toSet)
    val dir = new File(embedBundlesDirectory, VltpackUtil.noLeadingSlash(VltpackUtil.noTrailingSlash(bundleInstallPath)))
    if (dir.isDirectory || dir.mkdirs()) {
      artifacts.foreach( copyToDir(dir, getLog)_ )
    } else {
      throw new MojoExecutionException("Failed to create directory: " + dir)
    }
  }
}