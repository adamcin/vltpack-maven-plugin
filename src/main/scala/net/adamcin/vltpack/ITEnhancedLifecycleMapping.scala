package net.adamcin.vltpack

import scala.collection.JavaConverters._
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.lifecycle.mapping.{Lifecycle, LifecycleMapping}
import org.codehaus.plexus.component.annotations.{Configuration, Requirement, Component}
import org.apache.maven.artifact.handler.ArtifactHandler

object ITEnhancedLifecycleMapping {
  final val preITPhase = List(
    "net.adamcin:vltpack-maven-plugin:IT-upload",
    "net.adamcin:vltpack-maven-plugin:IT-upload-tests",
    "net.adamcin:vltpack-maven-plugin:IT-server-ready",
    "net.adamcin:vltpack-maven-plugin:IT-http-properties"
  ).mkString(",")
  final val ROLE = classOf[LifecycleMapping]
  final val ROLE_HINT = "content-package-with-IT"
}

@Component(role = ITEnhancedLifecycleMapping.ROLE, hint = ITEnhancedLifecycleMapping.ROLE_HINT)
class ITEnhancedLifecycleMapping extends LifecycleMapping {

  @Requirement(role = ITEnhancedLifecycleMapping.ROLE, hint = "content-package")
  var cplm: LifecycleMapping = null

  def getLifecycles: java.util.Map[String, Lifecycle] = {
    cplm.getLifecycles.asScala.map(transformDefaultLifecyle).asJava
  }

  def transformDefaultLifecyle(lifecycle: (String, Lifecycle)): (String, Lifecycle) = {
    val (name, value) = lifecycle
    if (name == "default") {
      val transformed = new Lifecycle
      transformed.setId(value.getId)
      val transMap = (Map.empty[String, String] ++ value.getPhases.asScala)
        .updated(LifecyclePhase.PRE_INTEGRATION_TEST.id, ITEnhancedLifecycleMapping.preITPhase)
      transformed.setPhases(transMap.asJava)
      (name, transformed)
    } else {
      lifecycle
    }
  }

  def getOptionalMojos(p1: String): java.util.List[String] = cplm.getOptionalMojos(p1)

  def getPhases(p1: String): java.util.Map[String, String] = cplm.getPhases(p1)
}

@Component(role = classOf[ArtifactHandler], hint = ITEnhancedLifecycleMapping.ROLE_HINT)
class ITEnhancedArtifactHandler extends ArtifactHandler {

  @Requirement(role = classOf[ArtifactHandler], hint = "content-package")
  var cpah: ArtifactHandler = null

  def getExtension: String = cpah.getExtension

  def getDirectory: String = cpah.getDirectory

  def getClassifier: String = cpah.getClassifier

  def getPackaging: String = cpah.getPackaging

  def isIncludesDependencies: Boolean = cpah.isIncludesDependencies

  def getLanguage: String = cpah.getLanguage

  def isAddedToClasspath: Boolean = cpah.isAddedToClasspath
}