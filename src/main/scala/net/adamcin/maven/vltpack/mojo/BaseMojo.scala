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

import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.logging.Log
import net.adamcin.maven.vltpack.LogsParameters
import org.apache.maven.plugins.annotations.{Component, Parameter}
import org.apache.maven.settings.Settings
import org.apache.maven.execution.MavenSession

/**
 * Base mojo class
 * @since 1.0
 * @author Mark Adamcin
 */
abstract class BaseMojo extends AbstractMojo with LogsParameters {

  /**
   * prints parameter values
   * @since 1.0
   */
  @Parameter(property = "vlt.debug")
  val debug = false

  @Component
  var settings: Settings = null

  @Component
  var session: MavenSession = null

  def execute() {
    if (debug) {
      printParams(getLog)
    }
  }

  override def printParams(log: Log) {
    super.printParams(log)
    log.info("vlt.debug = " + debug)
  }
}