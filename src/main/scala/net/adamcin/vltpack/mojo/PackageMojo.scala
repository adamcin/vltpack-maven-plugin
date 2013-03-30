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

import java.io.File
import org.apache.maven.plugins.annotations.{Parameter, LifecyclePhase, Mojo}
import net.adamcin.vltpack._
import scala.Some
import scalax.io.Resource

/**
 * Creates a vault package based on the generated metadata and configured content root
 * @since 0.6.0
 * @author Mark Adamcin
 */
@Mojo(name = "package",
  defaultPhase = LifecyclePhase.PACKAGE,
  threadSafe = true)
class PackageMojo
  extends BaseMojo
  with OutputParameters
  with CreatesPackage
  with IdentifiesPackages {

  final val DEFAULT_VLT_ROOT = "${project.build.outputDirectory}"

  /**
   * Content root directory containing jcr_root and META-INF folders
   */
  @Parameter(defaultValue = DEFAULT_VLT_ROOT)
  val vltRoot: File = null

  lazy val packageChecksum = (new ChecksumCalculator).add(jcrPath).add(vltRoot).calculate()

  def shouldCreatePackage: Boolean = {
    !targetFile.exists() ||
      !packageSha.exists() ||
      Resource.fromFile(packageSha).string != packageChecksum ||
      inputFileModified(packageSha, listFiles(vltRoot)) ||
      inputFileModified(packageSha, listFiles(vaultInfMetaInfDirectory)) ||
      inputFileModified(packageSha, listFiles(embedBundlesDirectory)) ||
      inputFileModified(packageSha, listFiles(embedPackagesDirectory))
  }

  override def execute() {
    super.execute()

    if (shouldCreatePackage) {
      overwriteFile(packageSha, packageChecksum)
      targetFile.delete()

      getLog.info("generating package")
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
    }

    project.getArtifact.setFile(targetFile)
  }
}