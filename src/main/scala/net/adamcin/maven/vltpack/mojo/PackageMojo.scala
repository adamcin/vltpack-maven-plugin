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

import java.io.File
import org.apache.maven.plugin.logging.Log
import net.adamcin.maven.vltpack._
import scala.Some
import org.apache.maven.plugins.annotations.{Parameter, LifecyclePhase, Mojo}

/**
 * Creates a vault package based on the generated metadata and configured content root
 * @since 1.0
 * @author Mark Adamcin
 */
@Mojo(name = "package",
  defaultPhase = LifecyclePhase.PACKAGE)
class PackageMojo
  extends BaseMojo
  with OutputParameters
  with CreatesPackage
  with IdentifiesPackages
  with BundlePathParameters {

  final val DEFAULT_VLT_ROOT = "${project.build.outputDirectory}"

  /**
   * Content root directory containing jcr_root and META-INF folders
   * @since 1.0
   */
  @Parameter(defaultValue = DEFAULT_VLT_ROOT)
  val vltRoot: File = null

  override def printParams(log: Log) {
    super.printParams(log)

    log.info("vltRoot = " + vltRoot)
  }


  override def execute() {
    super.execute()

    targetFile.delete()

    getLog.info("generating package " + targetFile)
    createPackage(vltRoot, targetFile, (zip) => {

      getLog.info("adding vault information...")
      val skipVaultEntries = addEntryToZipFile(
        addToSkip = true,
        skipEntries = Set.empty[String],
        entryFile = vaultInfMetaInfDirectory,
        entryName = "META-INF",
        zip = zip
      )

      getLog.info("adding embedded bundles...")
      val skipBundleEntries = addEntryToZipFile(
        addToSkip = true,
        skipEntries = skipVaultEntries,
        entryFile = embedBundlesDirectory,
        entryName = JCR_ROOT,
        zip = zip
      )

      getLog.info("adding embedded packages...")
      val skipPackageEntries = embedPackagesDirectory.listFiles.foldLeft(skipBundleEntries) {
        (skip, vp) => {
          identifyPackage(vp) match {
            case None => skip
            case Some(id) => {
              addEntryToZipFile(
                addToSkip = true,
                skipEntries = skip,
                entryFile = vp,
                entryName = JCR_ROOT + VltpackUtil.leadingSlashIfNotEmpty(VltpackUtil.noTrailingSlash(id.getInstallationPath)),
                zip = zip
              )
            }
          }
        }
      }

      skipPackageEntries
    })

    project.getArtifact.setFile(targetFile)
  }
}