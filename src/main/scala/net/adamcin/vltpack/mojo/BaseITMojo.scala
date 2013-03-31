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

import org.apache.maven.plugins.annotations.Parameter
import net.adamcin.vltpack.RequiresProject

/**
 * Base project IT mojo defining common mojo parameters and methods for enabling/disabling traffic to the configured
 * integration test server for goals bound to the integration test phase of the default vltpack lifecycle
 * @since 1.0.0
 * @author Mark Adamcin
 */
class BaseITMojo
  extends BaseMojo
  with RequiresProject {

  /**
   * Set this property to true to enable the pre-integration-test goals ({@code vltpack-maven-plugin:IT-*})
   */
  @Parameter(property = "vltpack.supportITs")
  val supportITs = false

  /**
   * By convention, this parameter is used to disable execution of the maven-failsafe-plugin.
   * It is recognized by vltpack to disable uploading of test artifacts and integration test reporting goals.
   */
  @Parameter(property = "skipITs")
  val skipITs = false

  /**
   * By convention, this parameter is used to disable execution of maven-surefire-plugin-derived goals.
   * It is recognized by vltpack to disable uploading of test artifacts and integration test reporting goals.
   */
  @Parameter(property = "skipTests")
  val skipTests = false

  override def skipOrExecute(skip: Boolean)(body: => Unit) {
    if (!supportITs || skip) {
      getLog.info("skipping [supportITs=" + supportITs + "][skip=" + skip + "]")
    } else {
      body
    }
  }

  def skipWithTestsOrExecute(skip: Boolean)(body: => Unit) {
    if (!supportITs || skip || skipITs || skipTests) {
      getLog.info("skipping [supportITs=" + supportITs + "][skip=" + skip + "][skipITs=" + skipITs + "][skipTests=" + skipTests + "]")
    } else {
      body
    }
  }
}