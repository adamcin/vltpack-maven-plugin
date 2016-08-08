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

package net.adamcin.vltpack

import java.util.Collections

import org.apache.jackrabbit.vault.packaging.Dependency
import org.apache.maven.plugins.annotations.Parameter

import scala.collection.JavaConversions._


/**
 * Trait defining common mojo parameters and methods useful for identifying package dependencies that
 * are not embedded in the main project artifact
 * @since 0.6.0
 * @author Mark Adamcin
 */
trait PackageDependencies
  extends RequiresProject
  with IdentifiesPackages
  with ResolvesArtifacts {

  /**
   * List of artifactIds matching dependencies that are valid vault packages
   */
  @Parameter
  var packageDependencies = Collections.emptyList[String]

  def packageDependencyArtifacts = resolveByArtifactIds(packageDependencies.toSet)

  def dependsOn: List[Dependency] = {
    packageDependencyArtifacts.flatMap {
      (artifact) => identifyPackage(artifact.getFile)
    }.map { new Dependency(_) }.toList
  }
}