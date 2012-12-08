package net.adamcin.maven.vltpack.mojo

import org.apache.maven.plugins.annotations._
import java.io.File
import org.apache.maven.plugin.logging.Log
import net.adamcin.maven.vltpack._
import scala.Some

/**
 *
 * @version $Id: PackageMojo.java$
 * @author madamcin
 */
@Mojo(
  name = "package",
  defaultPhase = LifecyclePhase.PACKAGE)
class PackageMojo
  extends VltpackLifecycleMojo
  with OutputParameters
  with CreatesPackage
  with IdentifiesPackages
  with BundlePathParameters {

  final val defaultVltRoot = "${project.build.outputDirectory}"

  @Parameter(defaultValue = defaultVltRoot)
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
            case Some(id) => {
              addEntryToZipFile(
                addToSkip = true,
                skipEntries = skip,
                entryFile = vp,
                entryName = JCR_ROOT + VltpackUtil.leadingSlashIfNotEmpty(VltpackUtil.noTrailingSlash(id.getInstallationPath)),
                zip = zip
              )
            }
            case None => skip
          }
        }
      }

      skipPackageEntries
    })

    project.getArtifact.setFile(targetFile)
  }
}