package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}
import java.io.File
import dispatch.{DefaultRequestVerbs, MethodVerbs, Http}

/**
 *
 * @version $Id: PutBundleFileMojo.java$
 * @author madamcin
 */
@Mojo(
  name = "put-bundle-file",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST,
  requiresProject = false,
  threadSafe = true)
class PutBundleFileMojo extends DeploysBundle with UploadParameters {

  @Parameter(property = "file", required = true)
  val file: File = null

  @Parameter(property = "vlt.skip.put-bundle-file")
  val skip = false

  def execute() {
    val resp = mkdirs(bundleInstallPath)
    getLog.info("Something: " + resp.getStatusCode + " " + resp.getStatusText)

    val somethingElse = Http(urlForPath(getBundleRepoPath(file.getName)) <<< file)

    val respElse = somethingElse()
    getLog.info("Something Else: " + respElse.getStatusCode + " " + respElse.getStatusText)
  }
}


