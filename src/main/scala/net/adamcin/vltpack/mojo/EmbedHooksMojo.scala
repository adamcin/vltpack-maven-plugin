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

import java.util.Collections

import net.adamcin.vltpack.{OutputParameters, ResolvesArtifacts}
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}

import scala.collection.JavaConversions._

/**
 * Embeds install hooks (dependencies referenced by artifactId) into the package. A valid
 * install hook is a jar with a Main-Class manifest attribute whose value references a class
 * in the jar that implements com.day.jcr.vault.packaging.InstallHook.
 * @since 0.6.0
 * @author Mark Adamcin
 */
@Mojo(name = "embed-hooks",
  defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
  threadSafe = true)
class EmbedHooksMojo
  extends BaseMojo
  with ResolvesArtifacts
  with OutputParameters {

  /**
   * List of articleIds matching dependencies that should be embedded
   */
  @Parameter
  val embedHooks = Collections.emptyList[String]

  override def execute() {
    super.execute()

    val artifacts = resolveByArtifactIds(embedHooks.toSet)
    val dir = hooksDirectory
    if (dir.isDirectory || dir.mkdirs()) {
      artifacts.foreach( copyToDir(dir, getLog)_ )
    } else {
      throw new MojoExecutionException("Failed to create directory: " + dir)
    }
  }

}