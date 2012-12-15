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

import mojo.BaseMojo
import org.apache.maven.repository.RepositorySystem
import org.apache.maven.artifact.repository.{DefaultRepositoryRequest, RepositoryRequest}
import org.apache.maven.artifact.Artifact
import org.apache.maven.plugin.logging.Log
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest
import org.slf4j.LoggerFactory
import collection.JavaConversions
import java.io.File
import scalax.io.Resource
import org.codehaus.plexus.util.SelectorUtils
import org.apache.maven.project.MavenProject
import org.apache.maven.plugins.annotations.Component

/**
 * Trait defining common mojo parameters and methods useful for arbitrarily resolving artifacts from
 * local and remote repositories
 * @since 1.0
 * @author Mark Adamcin
 */
trait ResolvesArtifacts extends BaseMojo {
  private val log = LoggerFactory.getLogger(getClass)

  @Component
  var repositorySystem: RepositorySystem = null

  def proj: MavenProject = Option(session) match {
    case Some(s) => s.getCurrentProject
    case None => null
  }

  lazy val repositoryRequest: RepositoryRequest = DefaultRepositoryRequest.getRepositoryRequest(session, proj)

  lazy val dependencies: List[Artifact] = Option(proj) match {
    case Some(project) => {
      JavaConversions.collectionAsScalaIterable(project.getDependencyArtifacts).toList
    }
    case None => Nil
  }

  def resolveByArtifactIds(artifactIds: Set[String]): Stream[Artifact] = {

    val deps = dependencies.filter { (artifact: Artifact) => artifactIds contains artifact.getArtifactId}

    resolveArtifacts(deps.toStream)
  }

  def resolveArtifacts(artifacts: Stream[Artifact]): Stream[Artifact] = {
    val request = new ArtifactResolutionRequest(repositoryRequest)
    artifacts.toStream.foldLeft(Stream.empty[Artifact]) {
      (stream, artifact: Artifact) => {
        request.setArtifact(artifact)
        val result = repositorySystem.resolve(request)

        stream #::: JavaConversions.collectionAsScalaIterable(result.getArtifacts).toStream
      }
    }
  }

  def resolveByFilter(filter: String): Stream[Artifact] = {
    val deps = dependencies.filter {
      (artifact: Artifact) => SelectorUtils.`match`(filter, artifact.getDependencyConflictId)
    }

    resolveArtifacts(deps.toStream)
  }

  def resolveByGATCV(groupId: String, artifactId: String, artifactType: String, classifier: String, version: String): Option[Artifact] = {
    val artifact = repositorySystem.createArtifactWithClassifier(groupId, artifactId, version, artifactType, classifier)
    Option(artifact) match {
      case Some(a) => resolveArtifacts(Stream(a)).headOption
      case None => None
    }
  }

  def copyToDir(dir: File, log: Log)(artifact: Artifact): File = {
    val target = new File(dir, artifact.getFile.getName)
    if (target.lastModified().compareTo(artifact.getFile.lastModified()) < 0) {
      log.info("Copying " + artifact.getId)
      log.info("\t to " + VltpackUtil.toRelative(new File("."), target.getAbsolutePath))
      Resource.fromFile(artifact.getFile).copyDataTo(Resource.fromFile(target))
    }
    target
  }
}