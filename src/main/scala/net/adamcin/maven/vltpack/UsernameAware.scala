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

package net.adamcin.maven.vltpack

import mojo.BaseMojo
import org.apache.maven.plugin.logging.Log
import org.apache.maven.plugins.annotations.Parameter

/**
 * Trait defining the vlt.user mojo parameter
 * @since 1.0
 * @author Mark Adamcin
 */
trait UsernameAware extends BaseMojo {

  final val DEFAULT_USER = "admin"

  @Parameter(property = "vlt.user", defaultValue = DEFAULT_USER)
  val user = DEFAULT_USER

  override def printParams(log: Log) {
    super.printParams(log)
    log.info("vlt.user = " + user)
  }
}