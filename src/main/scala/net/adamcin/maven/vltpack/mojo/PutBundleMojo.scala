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


import org.apache.maven.plugin.logging.Log
import net.adamcin.maven.vltpack.{RequiresProject, DeploysWithBuild, PutsBundle}
import scala.Left
import scala.Right
import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}

/**
 * PUT the project artifact (of packaging type 'bundle') to the configured CQ server
 * @since 1.0
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
   * @since 1.0
   */
  @Parameter(property = "vlt.skip.put-bundle")
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

  override def printParams(log: Log) {
    super.printParams(log)
    getLog.info("vlt.skip.put-bundle = " + skip)
  }
}