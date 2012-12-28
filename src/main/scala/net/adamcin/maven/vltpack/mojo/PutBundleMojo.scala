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


import net.adamcin.maven.vltpack.{RequiresProject, DeploysWithBuild, PutsBundle}
import scala.Left
import scala.Right
import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}

/**
 * PUT the project artifact (of packaging type 'bundle') to the configured CQ server
 * @since 0.6.0
 * @author Mark Adamcin
 */
@Mojo(
  name = "put-bundle",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST,
  threadSafe = true)
class PutBundleMojo
  extends BaseMojo
  with RequiresProject
  with PutsBundle
  with DeploysWithBuild {

  /**
   * Set to true to skip the execution of this mojo
   */
  @Parameter(property = "vltpack.skip.put-bundle")
  val skip = false

  override def execute() {
    super.execute()

    if (!deploy || skip || project.getPackaging != "bundle") {
      getLog.info("skipping [deploy=" + deploy + "][skip=" + skip + "][packaging=" + project.getPackaging + "]")
    } else {
      putBundle(project.getArtifact.getFile) match {
        case Right(t) => throw t
        case Left(messages) => messages.foreach { getLog.info(_) }
      }
    }
  }
}