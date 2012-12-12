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
import net.adamcin.maven.vltpack.{BundlePathParameters, VltpackUtil, OutputParameters, ResolvesArtifacts}
import java.io.File
import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}

/**
 * Embeds bundles in the project artifact at the configured bundleInstallPath
 * @since 1.0
 * @author Mark Adamcin
 */
@Mojo(name = "embed-bundles",
  defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
class EmbedBundlesMojo
  extends BaseMojo
  with ResolvesArtifacts
  with BundlePathParameters
  with OutputParameters {

  /**
   * List of articleIds matching dependencies that should be embedded
   * @since 1.0
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

  override def printParams(log: Log) {
    super.printParams(log)

    log.info("embedBundles:")
    JavaConversions.collectionAsScalaIterable(embedBundles).foreach {
      (embedBundle) => log.info("  " + embedBundle)
    }
  }
}