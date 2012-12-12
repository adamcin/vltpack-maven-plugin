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

import java.io.File
import mojo.BaseMojo
import org.apache.maven.plugin.logging.Log
import org.apache.maven.plugins.annotations.Parameter

/**
 * Trait defining common parameters and methods for placement of bundles within a JCR repository
 * @since 1.0
 * @author Mark Adamcin
 */
trait BundlePathParameters extends BaseMojo {

  final val defaultBundleInstallPath = "/apps/bundles/install/30"

  /**
   * Set the JCR path where bundles will be installed for this project. Use a numeric suffix
   * (as in "/apps/myapp/install/30") to apply a felix start level configuration to the bundles
   * @since 1.0
   */
  @Parameter(defaultValue = defaultBundleInstallPath)
  var bundleInstallPath: String = defaultBundleInstallPath

  def getBundleRepoPath(filename: String): String = {
    bundleInstallPath + "/" + filename
  }

  def getBundlePath(file: File): String = getBundleRepoPath(file.getName)

  override def printParams(log: Log) {
    super.printParams(log)

    log.info("bundleInstallPath = " + bundleInstallPath)
  }

}