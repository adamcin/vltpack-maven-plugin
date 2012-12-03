package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations._
import java.io.File
import org.apache.maven.plugin.logging.Log

/**
 *
 * @version $Id: PackageMojo.java$
 * @author madamcin
 */
@Mojo(
  name = "package",
  defaultPhase = LifecyclePhase.PACKAGE)
class PackageMojo extends BaseMojo with OutputParameters with CreatesPackage with IdentifiesPackages with BundlePathParameters {

  final val defaultVltRoot = "${project.build.outputDirectory}"

  @Parameter(defaultValue = defaultVltRoot)
  val vltRoot: File = null

  override def printParams(log: Log) {
    super.printParams(log)

    log.info("vltRoot = " + vltRoot)
  }

  lazy val targetFile: File = new File(project.getBuild.getDirectory + "/" + project.getBuild.getFinalName + ".zip")

  override def execute() {
    super.execute()

    getLog.info("generating package " + targetFile)
    createPackage(vltRoot, targetFile, (zip) => {

      getLog.info("adding vault information...")
      val skipVaultEntries = addEntryToZipFile(
        addToSkip = true,
        skipEntries = Set.empty[String],
        entryFile = vaultInfDirectory,
        entryName = noLeadingSlash(noTrailingSlash(vaultPrefix)),
        zip = zip
      )

      getLog.info("adding embedded bundles...")
      val skipBundleEntries = addEntryToZipFile(
        addToSkip = true,
        skipEntries = skipVaultEntries,
        entryFile = embedBundlesDirectory,
        entryName = JCR_ROOT + leadingSlashIfNotEmpty(noTrailingSlash(bundleInstallPath)),
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
                entryName = JCR_ROOT + leadingSlashIfNotEmpty(noTrailingSlash(id.getInstallationPath)),
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