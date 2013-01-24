package net.adamcin.vltpack.mojo

import org.apache.maven.plugins.annotations.{Parameter, LifecyclePhase, Mojo}
import net.adamcin.vltpack.{OutputParameters, ResolvesArtifacts}
import java.util.Collections
import scala.collection.JavaConversions._
import org.apache.maven.plugin.MojoExecutionException

/**
 * Embeds install hooks (dependencies referenced by artifactId) into the package. A valid
 * install hook is a jar with a Main-Class manifest attribute whose value references a class
 * in the jar that implements com.day.jcr.vault.packaging.InstallHook.
 * @since 0.6.0
 * @author Mark Adamcin
 */
@Mojo(name = "embed-hooks",
  defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
  threadSafe = true)
class EmbedHooksMojo
  extends BaseMojo
  with ResolvesArtifacts
  with OutputParameters {

  /**
   * List of articleIds matching dependencies that should be embedded
   */
  @Parameter
  val embedHooks = Collections.emptyList[String]

  override def execute() {
    super.execute()

    val artifacts = resolveByArtifactIds(embedHooks.toSet)
    val dir = hooksDirectory
    if (dir.isDirectory || dir.mkdirs()) {
      artifacts.foreach( copyToDir(dir, getLog)_ )
    } else {
      throw new MojoExecutionException("Failed to create directory: " + dir)
    }
  }

}