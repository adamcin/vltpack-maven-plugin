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

import collection.JavaConversions
import org.apache.maven.plugin.logging.Log
import java.util.Collections
import org.apache.maven.plugin.MojoExecutionException
import net.adamcin.maven.vltpack.{OutputParameters, ResolvesArtifacts}
import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}

/**
 * Embed sub packages in the project artifact package under /etc/packages
 * @since 1.0
 * @author Mark Adamcin
 */
@Mojo(name = "embed-packages",
  defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
class EmbedPackagesMojo
  extends BaseMojo
  with ResolvesArtifacts
  with OutputParameters {

  /**
   * List of artifactIds matching project dependencies that should be embedded
   * @since 1.0
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

  override def printParams(log: Log) {
    super.printParams(log)

    log.info("embedPackages:")
    JavaConversions.collectionAsScalaIterable(embedPackages).foreach {
      (embedPackage) => log.info("  " + embedPackage)
    }
  }
}