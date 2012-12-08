package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.Parameter
import com.day.jcr.vault.packaging.Dependency
import collection.JavaConversions


/**
 *
 * @version $Id: PackageDependencies.java$
 * @author madamcin
 */
trait PackageDependencies
  extends RequiresProject
  with IdentifiesPackages
  with ResolvesArtifacts {

  @Parameter
  val packageDependencies = java.util.Collections.emptyList[String]

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