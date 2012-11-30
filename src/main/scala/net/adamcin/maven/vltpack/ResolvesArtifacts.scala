package net.adamcin.maven.vltpack

import org.apache.maven.repository.RepositorySystem
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.artifact.repository.{DefaultRepositoryRequest, RepositoryRequest}
import org.apache.maven.artifact.Artifact
import org.apache.maven.plugin.logging.Log
import org.apache.maven.artifact.resolver.{ResolutionListener, ArtifactResolutionRequest, ArtifactResolutionResult}
import org.apache.maven.artifact.resolver.filter.ArtifactFilter
import org.apache.maven.shared.artifact.filter.collection.{ArtifactIdFilter, TypeFilter}
import org.slf4j.LoggerFactory
import org.apache.maven.artifact.versioning.VersionRange
import collection.JavaConversions

/**
 *
 * @version $Id: ResolvesArtifacts.java$
 * @author madamcin
 */
trait ResolvesArtifacts extends RequiresProject with LogsParameters {
  val log = LoggerFactory.getLogger(getClass)

  @Component
  var repositorySystem: RepositorySystem = null

  lazy val repositoryRequest: RepositoryRequest = DefaultRepositoryRequest.getRepositoryRequest(session, project)

  lazy val dependencies = JavaConversions.collectionAsScalaIterable(project.getDependencyArtifacts).toList

  def resolveByArtifactIds(artifactIds: Set[String]): Stream[Artifact] = {

    val deps = dependencies.filter { (artifact) => artifactIds contains artifact.getArtifactId }

    val request = new ArtifactResolutionRequest(repositoryRequest)

    deps.toStream.foldLeft(Stream.empty[Artifact]) {
      (stream, artifact) => {
        request.setArtifact(artifact)
        val result = repositorySystem.resolve(request)

        stream #::: JavaConversions.collectionAsScalaIterable(result.getArtifacts).toStream
      }
    }
  }



  override def printParams(log: Log) {
    super.printParams(log)


    log.info("repositorySystem is empty? " + Option(repositorySystem).isEmpty)
    log.info("repositoryRequest is empty? " + Option(repositoryRequest).isEmpty)
  }
}