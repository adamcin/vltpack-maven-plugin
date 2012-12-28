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

import java.io.File
import com.day.jcr.vault.packaging.{VaultPackage, PackageId}
import java.util.jar.{JarEntry, JarFile}
import scalax.io.Resource
import java.util.Properties

/**
 * Companion object for the trait defining these useful property constants
 * @since 0.6.0
 * @author Mark Adamcin
 */
object IdentifiesPackages {
  final val GROUP = VaultPackage.NAME_GROUP
  final val NAME = VaultPackage.NAME_NAME
  final val VERSION = VaultPackage.NAME_VERSION
  final val PATH = VaultPackage.NAME_PATH
  final val DESCRIPTION = VaultPackage.NAME_DESCRIPTION
  final val CREATED = VaultPackage.NAME_CREATED
  final val CREATED_BY = VaultPackage.NAME_CREATED_BY
  final val DEPENDENCIES = VaultPackage.NAME_DEPENDENCIES
}

/**
 * Trait defining common mojo parameters and methods needed to identify vault package binaries based on
 * embedded metadata
 * @since 0.6.0
 * @author Mark Adamcin
 */
trait IdentifiesPackages {

  val propertiesEntry = "META-INF/vault/properties.xml"

  def identifyPackage(file: File): Option[PackageId] = {
    def jarEntryOpener(jar: JarFile)(entry: JarEntry) = jar.getInputStream(entry)

    Option(file) match {
      case Some(f) => {
        if (!file.exists() || file.isDirectory) {
          Option.empty[PackageId]
        } else {
          try {
            val jar = new JarFile(file)
            val opener = jarEntryOpener(jar)_
            val defaultId = Option(new PackageId(PackageId.ETC_PACKAGES_PREFIX + file.getName))
            Option(jar.getJarEntry(propertiesEntry)) match {
              case Some(entry) => {
                Resource.fromInputStream(opener(entry)).addCloseAction(VltpackUtil.inputCloser).acquireAndGet {
                  (f) => {
                    val props = new Properties
                    props.loadFromXML(f)
                    getIdFromProperties(props, defaultId)
                  }
                }
              }
              case None => defaultId
            }

          } catch {
            case t: Throwable => Option.empty[PackageId]
          }
        }
      }
      case None => None
    }
  }

  def getIdFromProperties(props: Properties, defaultId: Option[PackageId]): Option[PackageId] = {
    import IdentifiesPackages._
    val version = Option(props.getProperty(VERSION)) match {
      case Some(value) => value
      case None => ""
    }

    (Option(props.getProperty(GROUP)), Option(props.getProperty(NAME))) match {
      case (Some(group), Some(name)) => Option(new PackageId(group, name, version))
      case _ => {
        val path = props.getProperty(PATH, "")
        if (path.startsWith(PackageId.ETC_PACKAGES_PREFIX)) {
          Option(new PackageId(path))
        } else {
          defaultId
        }
      }
    }
  }
}