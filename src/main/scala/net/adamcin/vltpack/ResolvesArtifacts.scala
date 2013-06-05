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

import mojo.BaseMojo
import org.apache.maven.repository.RepositorySystem
import org.apache.maven.artifact.repository.{DefaultRepositoryRequest, RepositoryRequest}
import org.apache.maven.artifact.Artifact
import org.apache.maven.plugin.logging.Log
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest
import org.slf4j.LoggerFactory
import collection.JavaConversions._
import java.io.File
import scalax.io.Resource
import org.codehaus.plexus.util.SelectorUtils
import org.apache.maven.project.MavenProject
import org.apache.maven.artifact.repository.ArtifactRepository
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.plugins.annotations.Parameter
import util.matching.Regex

/**
 * Trait defining common mojo parameters and methods useful for arbitrarily resolving artifacts from
 * local and remote repositories
 * @since 0.6.0
 * @author Mark Adamcin
 */
trait ResolvesArtifacts extends BaseMojo {
  private val log = LoggerFactory.getLogger(getClass)

  final val pA = new Regex("""^([^:]*)$""",
    "artifactId")

  final val pGA = new Regex("""^([^:]*):([^:]*)$""",
    "groupId", "artifactId")

  final val pGAV = new Regex("""^([^:]*):([^:]*):([^:]*)$""",
    "groupId", "artifactId", "version")

  final val pGATV = new Regex("""^([^:]*):([^:]*):([^:]*):([^:]*)$""",
    "groupId", "artifactId", "artifactType", "version")

  final val pGATCV = new Regex("""^([^:]*):([^:]*):([^:]*):([^:]*):([^:]*)$""",
    "groupId", "artifactId", "artifactType", "classifier", "version")

  /**
   * Specify the local repository path for resolved artifacts
   * Refer to maven-install-plugin:install-file
   * @since 1.0.4
   */
  @Parameter(property = "vltpack.localRepositoryPath")
  val localRepositoryPath: File = null

  @Component
  var repositorySystem: RepositorySystem = null

  def proj: MavenProject = Option(session) match {
    case Some(s) => s.getCurrentProject
    case None => null
  }

  lazy val localRepository: ArtifactRepository =
    Option(localRepositoryPath) match {
      case Some(path) => {
        repositorySystem.createLocalRepository(path)
      }
      case None => {
        Option(session) match {
          case Some(s) => session.getLocalRepository
          case None => repositorySystem.createDefaultLocalRepository()
        }
      }
    }

  lazy val repositoryRequest: RepositoryRequest = {
    val request = DefaultRepositoryRequest.getRepositoryRequest(session, proj)
    request.setLocalRepository(localRepository)
    request
  }

  lazy val dependencies: List[Artifact] = Option(proj) match {
    case Some(project) => {
      project.getDependencyArtifacts.toList
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

        stream #::: result.getArtifacts.toStream
      }
    }
  }

  def resolveByFilter(filter: String): Stream[Artifact] = {
    val deps = dependencies.filter {
      (artifact: Artifact) => SelectorUtils.`match`(filter, artifact.getDependencyConflictId)
    }

    resolveArtifacts(deps.toStream)
  }

  def resolveByCoordinates(coordinates: String): Option[Artifact] = {
    Option(coordinates).flatMap {
      (coords) => {
        coords.count(_ == ':') match {
          case 2 => pGAV findFirstIn coords match {
            case Some(pGAV(groupId, artifactId, version)) =>
              resolveByCoordinates(groupId, artifactId, version, "jar", null)
            case None => None
          }
          case 3 => pGATV findFirstIn coords match {
            case Some(pGATV(groupId, artifactId, artifactType, version)) =>
              resolveByCoordinates(groupId, artifactId, version, artifactType, null)
            case None => None
          }
          case 4 => pGATCV findFirstIn coords match {
              case Some(pGATCV(groupId, artifactId, classifier, artifactType, version)) =>
                resolveByCoordinates(groupId, artifactId, version, artifactType, classifier)
              case None => None
          }
          case _ => None
        }
      }
    }
  }

  def resolveByCoordinates(groupId: String, artifactId: String, version: String, artifactType: String, classifier: String): Option[Artifact] = {
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