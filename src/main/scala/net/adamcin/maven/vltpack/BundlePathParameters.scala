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
import mojo.BaseMojo
import org.apache.maven.plugins.annotations.Parameter

/**
 * Trait defining common parameters and methods for placement of bundles within a JCR repository
 * @since 0.6.0
 * @author Mark Adamcin
 */
trait BundlePathParameters extends BaseMojo {

  final val defaultBundleInstallPath = "/apps/bundles/install/30"

  /**
   * Set the JCR path where bundles will be installed for this project. Use a numeric suffix
   * (as in "/apps/myapp/install/30") to apply a felix start level configuration to the bundles
   */
  @Parameter(defaultValue = defaultBundleInstallPath)
  var bundleInstallPath: String = defaultBundleInstallPath

  def getBundleRepoPath(filename: String): String = {
    bundleInstallPath + "/" + filename
  }

  def getBundlePath(file: File): String = getBundleRepoPath(file.getName)
}