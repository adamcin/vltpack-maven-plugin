package net.adamcin.maven.vltpack

import java.io.File
import com.day.jcr.vault.packaging.PackageId
import java.util.jar.{JarEntry, JarFile}
import scalax.io.Resource
import java.util.Properties

/**
 *
 * @version $Id: IdentifiesPackages.java$
 * @author madamcin
 */
trait IdentifiesPackages {

  val propertiesEntry = "META-INF/vault/properties.xml"

  def identifyPackage(file: File): Option[PackageId] = {
    def jarEntryOpener(jar: JarFile)(entry: JarEntry) = jar.getInputStream(entry)

    if (!file.exists() || file.isDirectory) {
      Option.empty[PackageId]
    } else {
      try {
        val jar = new JarFile(file)
        val opener = jarEntryOpener(jar)_
        val defaultId = Option(new PackageId(PackageId.ETC_PACKAGES_PREFIX + file.getName))
        Option(jar.getJarEntry(propertiesEntry)) match {
          case Some(entry) => {
            Resource.fromInputStream(opener(entry)).addCloseAction(IOUtil.inputCloser).acquireAndGet {
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

  def getIdFromProperties(props: Properties, defaultId: Option[PackageId]): Option[PackageId] = {
    val version = Option(props.getProperty("version")) match {
      case Some(value) => value
      case None => ""
    }

    (Option(props.getProperty("group")), Option(props.getProperty("name"))) match {
      case (Some(group), Some(name)) => Option(new PackageId(group, name, version))
      case _ => {
        val path = props.getProperty("path", "")
        if (path.startsWith(PackageId.ETC_PACKAGES_PREFIX)) {
          Option(new PackageId(path))
        } else {
          defaultId
        }
      }
    }
  }
}