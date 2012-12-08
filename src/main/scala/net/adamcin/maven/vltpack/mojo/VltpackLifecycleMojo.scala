package net.adamcin.maven.vltpack.mojo

import java.io.File
import net.adamcin.maven.vltpack.RequiresProject

/**
 *
 * @version $Id: VltpackLifecycleMojo.java$
 * @author madamcin
 */
abstract class VltpackLifecycleMojo extends BaseMojo with RequiresProject {

  lazy val targetFile: File = new File(project.getBuild.getDirectory + "/" + project.getBuild.getFinalName + ".zip")
}