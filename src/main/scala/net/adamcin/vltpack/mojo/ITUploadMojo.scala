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

import net.adamcin.vltpack.{ChecksumCalculator, OutputParameters, PackageDependencies, UploadsPackages}
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}

import scala.collection.JavaConversions._

/**
 * Uploads the project vault package and its dependencies to the configured IT server
 * @since 1.0.0
 * @author Mark Adamcin
 */
@Mojo(name = "IT-upload",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST,
  threadSafe = true)
class ITUploadMojo
  extends BaseITMojo
  with OutputParameters
  with UploadsPackages
  with PackageDependencies {

  /**
   * Set to true to skip mojo execution
   */
  @Parameter(property = "vltpack.skip.IT-upload")
  var skip = false

  lazy val uploadChecksum = {
    val calc = new ChecksumCalculator
    packageDependencies.foreach { calc.add }
    calc.calculate()
  }

  def shouldForceUpload = {
    force || !uploadSha.exists() ||
    inputFileModified(uploadSha, List(targetFile))
  }

  override def execute() {
    super.execute()

    skipOrExecute(skip) {
      if (!packageDependencies.isEmpty) {
        getLog.info("uploading package dependencies...")
        packageDependencyArtifacts.foreach { uploadPackageArtifact }
      }

      getLog.info("uploading main package...")
      uploadPackageArtifact(project.getArtifact)(shouldForceUpload)

      overwriteFile(uploadSha, uploadChecksum)
    }
  }
}