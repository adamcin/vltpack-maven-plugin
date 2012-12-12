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
import org.slf4j.LoggerFactory

/**
 * Trait defining common mojo parameters and methods useful for creation of the
 * @since 1.0
 * @author Mark Adamcin
 */
trait OutputParameters extends RequiresProject {
  private val log = LoggerFactory.getLogger(getClass)

  /**
   * target directory
   */
  lazy val outputDirectory = getExistingDir(new File(project.getBuild.getDirectory))

  /**
   * target vltpack file
   */
  lazy val targetFile: File = new File(outputDirectory, project.getBuild.getFinalName + ".zip")

  /**
   * directory containing vltpack-generated files
   */
  lazy val vltpackDirectory = getExistingDir(new File(outputDirectory, "vltpack"))

  /**
   * directory containing resolved bundles
   */
  lazy val embedBundlesDirectory = getExistingDir(new File(vltpackDirectory, "embed-bundles"))


  def relativeToBundleInstallPath(bundle: File): String = {
    VltpackUtil.toRelative(embedBundlesDirectory, bundle.getPath)
  }

  /**
   * directory containing resolved packages
   */
  lazy val embedPackagesDirectory = getExistingDir(new File(vltpackDirectory, "embed-packages"))
  lazy val vaultInfDirectory = getExistingDir(new File(vltpackDirectory, "vault-inf"))
  lazy val transientRepoDirectory = getExistingDir(new File(vaultInfDirectory, "definitionRepo"))

  /**
   * vault-inf-generated META-INF/vault/... resources
   */
  lazy val vaultInfMetaInfDirectory = getExistingDir(new File(vaultInfDirectory, "META-INF"))
  lazy val vaultDirectory = getExistingDir(new File(vaultInfMetaInfDirectory, "vault"))
  lazy val configXml = new File(vaultDirectory, "config.xml")
  lazy val settingsXml = new File(vaultDirectory, "settings.xml")
  lazy val filterXml = new File(vaultDirectory, "filter.xml")
  lazy val propertiesXml = new File(vaultDirectory, "properties.xml")

  lazy val definitionDirectory = getExistingDir(new File(vaultDirectory, "definition"))
  lazy val definitionXml = new File(definitionDirectory, ".content.xml")

  lazy val thumbnailDirectory = getExistingDir(new File(definitionDirectory, "thumbnail"))
  lazy val thumbnailFileDirectory = getExistingDir(new File(thumbnailDirectory, "file"))
  lazy val thumbnailFileXml = getExistingDir(new File(thumbnailFileDirectory, ".content.xml"))

  def getExistingDir(file: File): File = {
    if (!file.exists() && !file.mkdir()) {
      log.error("[getExistingDir] failed to create directory: {}", file)
    }
    file
  }


}