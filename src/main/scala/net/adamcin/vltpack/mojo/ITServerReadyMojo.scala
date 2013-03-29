package net.adamcin.vltpack.mojo

import net.adamcin.vltpack.HttpParameters
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}
import org.apache.maven.plugin.MojoFailureException
import annotation.tailrec
import dispatch._

/**
 *
 * @author Mark Adamcin
 * @since 1.0.0
 */
@Mojo(
  name = "IT-server-ready",
  defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
  threadSafe = true
)
class ITServerReadyMojo
  extends BaseITMojo
  with HttpParameters {

  @Parameter(property = "vltpack.skip.IT-server-ready")
  val skip = false

  @Parameter(defaultValue = "60")
  val timeout = 60

  @Parameter(defaultValue = "/libs/granite/core/content/login.html")
  val serverReadyPath = "/"

  @Parameter(defaultValue = "QUICKSTART_HOMEPAGE")
  val expectedContent = ""

  def checkContent(response: Promise[String]): Promise[Boolean] = {
    response.fold(
      (ex) => {
        getLog.info("server ready check exception: " + ex.getMessage)
        false
      },
      (content) => if (content contains expectedContent) {
        getLog.info("server ready check succeeded")
        true
      } else {
        getLog.info("server ready check failed to return expected content")
        false
      }
    )
  }

  override def execute() {
    skipWithTestsOrExecute(skip) {
      val until = System.currentTimeMillis() + (timeout * 1000)
      if (!waitForResponse(0)(until, () => urlForPath(serverReadyPath).subject OK as.String, checkContent)) {
        throw new MojoFailureException("Server failed to respond as expected within " + timeout + " seconds.")
      }
    }
  }
}
