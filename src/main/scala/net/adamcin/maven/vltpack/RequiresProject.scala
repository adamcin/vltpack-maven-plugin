package net.adamcin.maven.vltpack

import org.apache.maven.project.MavenProject
import org.apache.maven.plugins.annotations.Component

/**
 *
 * @version $Id: RequiresProject.java$
 * @author madamcin
 */
trait RequiresProject {

  @Component
  var project: MavenProject = null
}