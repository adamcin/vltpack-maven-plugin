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

import java.io.{FileOutputStream, File}
import mojo.BaseMojo
import org.apache.maven.plugin.MojoExecutionException
import scalax.io.Resource
import java.util.TimeZone
import java.util.jar.{JarEntry, JarOutputStream}
import org.slf4j.LoggerFactory
import com.day.jcr.vault.vlt.VltDirectory
import org.apache.maven.plugins.annotations.Parameter


/**
 * Trait defining common mojo parameters and methods for the creation of vault packages
 * @since 1.0
 * @author Mark Adamcin
 */
trait CreatesPackage
  extends BaseMojo {
  val log = LoggerFactory.getLogger(getClass)

  final val DEFAULT_JCR_PATH = "/"
  final val VAULT_PREFIX = "META-INF/vault/"
  final val JCR_ROOT = "jcr_root"
  final val META_INF = "META-INF"

  /**
   * Specify the JCR path that the jcr_root folder maps to. For instance, if the content was checked out
   * using the vlt command, a jcrPath parameter may have been explicitly specified as something other than
   * '/'. In this case, the same explicit value must be set for this mojo to properly create Jar entries so
   * that the package contents wind up at the correct path when installed
   * @since 1.0
   */
  @Parameter(property = "jcrPath", defaultValue = DEFAULT_JCR_PATH)
  var jcrPath: String = DEFAULT_JCR_PATH

  lazy val jcrPathNoSlashEnd = VltpackUtil.noLeadingSlash(VltpackUtil.noTrailingSlash(jcrPath))

  /**
   * Specify the server timezone if different from the local timezone. When CRX installs the package, it will
   * check the lastmodified timestamps on the Jar entries and compare them to the jcr:lastModified properties
   * in the repository to determine if each file should be installed or not. Because the Jar entry timestamp
   * does not account for timezone, it is possible for packages created in different timezones to be installed
   * inconsistently on the same server. By setting this parameter, the Jar entry timestamps will be adjusted to
   * reflect the local time of the server timezone instead of the local timezone
   *
   * The value of this property will be passed to <code>TimeZone.getTimeZone(String id)</code>, which will be
   * compared against the build machine's default time zone, <code>TimeZone.getDefault()</code>, for computing
   * the desired timestamp offset.
   *
   * As described in the TimeZone javadocs, if the value of this property is
   * not empty and is not a valid TimeZone id, the GMT timezone will be used
   * for adjustment of timestamps.
   * @since 1.0
   */
  @Parameter(property = "vltpack.tz")
  var serverTimezone: String = null

  val localTz = TimeZone.getDefault

  lazy val serverTz = Option(serverTimezone) match {
    case Some(tz) => TimeZone.getTimeZone(tz)
    case None => TimeZone.getDefault
  }

  def adjustToServerTimeZone(localTime: Long): Long =
    (localTime - localTz.getOffset(localTime)) + serverTz.getOffset(localTime)

  /**
   * Creates a package from a standard vlt working copy, such that the vltRoot directory has two children, jcr_root and
   * META-INF.
   * @param vltRoot parent of both jcr_root and META-INF
   * @param zipFile zip file to write to
   * @param prepareZipFile function that prepares the stream by, say, injecting generated versions of entries and
   *                      returning a Set of those entry names that should be skipped during normal package generation,
   *                      thus allowing one to merge multiple source trees
   */
  def createPackage(
                     vltRoot: File,
                     zipFile: File,
                     prepareZipFile: (JarOutputStream) => Set[String]) {

    if (zipFile.exists) {
      throw new MojoExecutionException("zipFile already exists")
    }

    val zipResource = Resource.fromOutputStream(new JarOutputStream(new FileOutputStream(zipFile)))

    zipResource.acquireFor {
      (zip) => {
        val skip = Option(prepareZipFile) match {
          case Some(f) => f(zip)
          case None => Set.empty[String]
        }

        addEntryToZipFile(
          addToSkip = false,
          skipEntries = skip,
          entryFile = new File(vltRoot, META_INF),
          entryName = META_INF,
          zip = zip)

        addEntryToZipFile(
          addToSkip = false,
          skipEntries = skip,
          entryFile = new File(vltRoot, JCR_ROOT),
          entryName = JCR_ROOT + VltpackUtil.leadingSlashIfNotEmpty(jcrPathNoSlashEnd),
          zip = zip)
      }
    } match {
      case Left(t :: ts) => throw t
      case _ =>
    }
  }

  /**
   * Recursively adds files to the provided zip output stream
   * @param addToSkip set to true to add the current entry to the returned set of entries to skip.
   * @param skipEntries entry names that should not be added to the zip file
   * @param entryFile file that will be copied to the stream if it is not a directory. if it is a directory, it's children
   *             will be listed and this method will be called on each as appropriate
   * @param entryName name of current entry should it be successfully added to the zip stream
   * @param zip zip output stream
   * @return set of zip entries to skip during subsequent calls to this method
   */
  def addEntryToZipFile(
                       addToSkip: Boolean,
                       skipEntries: Set[String],
                       entryFile: File,
                       entryName: String,
                       zip: JarOutputStream): Set[String] = {

    if (entryFile.isDirectory) {
      entryFile.listFiles().filter { _.getName != VltDirectory.META_DIR_NAME }.foldLeft(skipEntries) {
        (skip, f) => addEntryToZipFile(addToSkip, skip, f, entryName + "/" + f.getName, zip)
      }
    } else {
      if (!entryFile.exists() || (skipEntries contains entryName)) {
        skipEntries
      } else {
        val entry = new JarEntry(entryName)

        if (entryFile.lastModified > 0) {
          entry.setTime(adjustToServerTimeZone(entryFile.lastModified))
        }

        zip.putNextEntry(entry)

        Resource.fromFile(entryFile).copyDataTo(Resource.fromOutputStream(VltpackUtil.blockClose(zip)))
        if (addToSkip) {
          skipEntries + entryName
        } else {
          skipEntries
        }
      }
    }
  }
}