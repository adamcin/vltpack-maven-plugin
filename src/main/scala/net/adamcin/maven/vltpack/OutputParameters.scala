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

package net.adamcin.maven.vltpack

import java.io.File
import org.slf4j.LoggerFactory

/**
 * Trait defining common output path variables
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