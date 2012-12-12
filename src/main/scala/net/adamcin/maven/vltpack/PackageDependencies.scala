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

import com.day.jcr.vault.packaging.Dependency
import collection.JavaConversions
import org.apache.maven.plugins.annotations.Parameter


/**
 * Trait defining common mojo parameters and methods useful for identifying package dependencies that
 * are not embedded in the main project artifact
 * @since 1.0
 * @author Mark Adamcin
 */
trait PackageDependencies
  extends RequiresProject
  with IdentifiesPackages
  with ResolvesArtifacts {

  /**
   * List of artifactIds matching dependencies that are valid vault packages
   * @since 1.0
   */
  @Parameter
  var packageDependencies = java.util.Collections.emptyList[String]

  def packageDependencyArtifacts = resolveByArtifactIds(JavaConversions.collectionAsScalaIterable(packageDependencies).toSet)

  def dependsOn: List[Dependency] = {
    packageDependencyArtifacts.map {
      (artifact) => {
        identifyPackage(artifact.getFile) match {
          case Some(id) => new Dependency(id)
          case None => null
        }
      }
    }.filter {(dep) => Option(dep).isDefined}.toList
  }
}