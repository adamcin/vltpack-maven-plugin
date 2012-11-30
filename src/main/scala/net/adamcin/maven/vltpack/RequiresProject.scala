package net.adamcin.maven.vltpack

import org.apache.maven.project.MavenProject
import org.apache.maven.plugins.annotations.Component
import org.apache.maven.execution.MavenSession

/**
 *
 * @version $Id: RequiresProject.java$
 * @author madamcin
 */
trait RequiresProject {

  @Component
  var project: MavenProject = null

  @Component
  var session: MavenSession = null
}