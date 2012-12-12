/*
 * Copyright 2012 Mark Adamcin
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.adamcin.maven.vltpack

import java.io.File
import com.day.jcr.vault.packaging.{VaultPackage, PackageId}
import java.util.jar.{JarEntry, JarFile}
import scalax.io.Resource
import java.util.Properties

/**
 * Companion object for the trait defining these useful property constants
 * @since 1.0
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
 * @since 1.0
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