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

package net.adamcin.vltpack.mojo

import java.io.File
import scalax.io.Resource
import java.util.{Calendar, Properties, Collections}
import com.day.jcr.vault.packaging._
import com.day.jcr.vault.fs.config.{MetaInf, DefaultMetaInf, DefaultWorkspaceFilter}
import com.day.jcr.vault.fs.api.PathFilterSet
import com.day.jcr.vault.fs.filter.DefaultPathFilter
import org.apache.jackrabbit.util.ISO8601
import javax.jcr.{Node, SimpleCredentials, Session}
import org.apache.jackrabbit.core.TransientRepository
import com.day.jcr.vault.packaging.impl.{JcrPackageDefinitionImpl, JcrPackageManagerImpl}
import com.day.jcr.vault.fs.io
import io.PlatformExporter
import com.day.jcr.vault.util.{Text, JcrConstants}
import collection.JavaConversions._
import java.security.{DigestInputStream, MessageDigest}
import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}
import net.adamcin.vltpack._
import scala.Left
import scala.Some

/**
 * Generates package meta information under META-INF/vault, including config.xml, properties.xml, filter.xml
 * and the package definition
 * @since 0.6.0
 * @author Mark Adamcin
 */
@Mojo(
  name = "vault-inf",
  defaultPhase = LifecyclePhase.PREPARE_PACKAGE,
  threadSafe = true)
class VaultInfMojo
  extends BaseMojo
  with UsernameAware
  with OutputParameters
  with BundlePathParameters
  with PackageDependencies
  with IdentifiesPackages {

  final val DEFAULT_VAULT_SOURCE = "${project.build.outputDirectory}/META-INF/vault"
  final val DEFAULT_CONFIG = "com/day/jcr/vault/fs/config/defaultConfig-1.1.xml"

  /**
   * Source folder for existing meta info files, such as are created and managed by the VLT working copy
   */
  @Parameter(defaultValue = DEFAULT_VAULT_SOURCE)
  val vaultSource: File = null

  /**
   * Package properties that will be set in the properties.xml file
   */
  @Parameter
  val properties = Collections.emptyMap[String, String]

  /**
   * Set to true to generate the JCR Package Definition. This is necessary if thumbnails, screenshots, or
   * definition properties need to be set within the package. Package creation may take significantly more
   * time than normal if this is set because a Jackrabbit TransientRepository will need to be started
   */
  @Parameter
  val createDefinition = false

  /**
   * Properties to be added to the JCR Package Definition node. These will NOT be added to the properties.xml file.
   * (requires createDefinition to be set to true)
   */
  @Parameter
  val definitionProperties = Collections.emptyMap[String, String]

  /**
   * Specify a PNG file that will be used as the thumbnail for the package in the CRX Package Manager
   * (requires createDefinition to be set to true)
   */
  @Parameter
  val thumbnail: File = null

  /**
   * Specify a list of PNG files that will be included as package screenshots in the CRX Package Manager
   * (requires createDefinition to be set to true)
   */
  @Parameter
  val screenshots = java.util.Collections.emptyList[File]

  lazy val signature: String = {
    Resource.fromURL(getClass.getResource("plugin.properties")).inputStream.acquireAndGet {
      (f) => {
        val props = new Properties
        props.load(f)
        "%s (%s:%s:%s)".format(
          props.getProperty("name"),
          props.getProperty("groupId"),
          props.getProperty("artifactId"),
          props.getProperty("version"))
      }
    }
  }

  override def execute() {
    super.execute()
    generateConfigXml()
    generateFilterXml()
    generatePropertiesXml()

    if (createDefinition) {
      generateDefinition()
    }
  }

  def getResourceFromClasspath(name: String) = {
    Resource.fromInputStream(getClass.getClassLoader.getResourceAsStream(name)).
      addCloseAction(VltpackUtil.inputCloser)
  }

  def generateFilterXml() {
    val file = new File(vaultSource, "filter.xml")

    val filter = new DefaultWorkspaceFilter

    if (file.exists()) {
      val sourceFilter = new DefaultWorkspaceFilter
      sourceFilter.load(file)
      filter.getFilterSets.addAll(sourceFilter.getFilterSets)
    }

    def listEmbedBundles(baseDir: File)(file: File): List[String] = {
      if (file.isDirectory) {
        file.listFiles().flatMap(listEmbedBundles(baseDir)(_)).toList
      } else {
        List("/" + VltpackUtil.toRelative(baseDir, file.getAbsolutePath))
      }
    }

    listEmbedBundles(embedBundlesDirectory)(embedBundlesDirectory).foreach {
      (path) => {
        if (!filter.contains(path)) {
          val bundleFilterSet =
            if (filter.covers(path)) {
              val oldFs = filter.getCoveringFilterSet(path)
              if (oldFs.isSealed) {
                val newFs = new PathFilterSet(oldFs.getRoot)
                newFs.setImportMode(oldFs.getImportMode)
                oldFs.getEntries.foreach( e =>
                  if (e.isInclude) newFs.addInclude(e.getFilter)
                  else newFs.addExclude(e.getFilter)
                )
                filter.getFilterSets.remove(oldFs)
                filter.getFilterSets.add(newFs)
                newFs
              } else {
                oldFs
              }
            } else {
              val set = new PathFilterSet(path)
              set.addExclude(new DefaultPathFilter(Text.getRelativeParent(path, 1) + "(/.*)?"))
              filter.getFilterSets.add(set)
              set
            }

          bundleFilterSet.addInclude(new DefaultPathFilter(path))
        }
      }
    }

    val embedPackages = embedPackagesDirectory.listFiles
    if (embedPackages.size > 0) {
      val packageFilterSet =
        if (filter.covers(PackageId.ETC_PACKAGES)) {
          val oldFs = filter.getCoveringFilterSet(PackageId.ETC_PACKAGES)
          if (oldFs.isSealed) {
            val newFs = new PathFilterSet(oldFs.getRoot)
            newFs.setImportMode(oldFs.getImportMode)
            oldFs.getEntries.foreach( e =>
              if (e.isInclude) newFs.addInclude(e.getFilter)
              else newFs.addExclude(e.getFilter)
            )
            filter.getFilterSets.remove(oldFs)
            filter.getFilterSets.add(newFs)
            newFs
          } else {
            oldFs
          }
        } else {
          val set = new PathFilterSet(PackageId.ETC_PACKAGES)
          set.addExclude(new DefaultPathFilter(PackageId.ETC_PACKAGES + "(/.*)?"))
          filter.getFilterSets.add(set)
          set
        }

      embedPackages.foreach {
        (pkg) => identifyPackage(pkg) match {
          case Some(id) => packageFilterSet.addInclude(new DefaultPathFilter(id.getInstallationPath + ".zip"))
          case None => getLog.warn("Failed to identify package: " + pkg)
        }
      }
    }

    getLog.info("generating " + filterXml)
    val filterResource = Resource.fromFile(filterXml)
    filterResource.truncate(0)
    Resource.fromInputStream(filter.getSource).addCloseAction(VltpackUtil.inputCloser).
      copyDataTo(filterResource)
  }

  def generatePropertiesXml() {
    import IdentifiesPackages._
    val props = new Properties()

    props.putAll(properties)

    props.setProperty(MetaInf.PACKAGE_FORMAT_VERSION, MetaInf.FORMAT_VERSION_2.toString)

    if (!props.containsKey(CREATED)) {
      props.setProperty(CREATED, ISO8601.format(Calendar.getInstance()))
    }

    if (!props.containsKey(CREATED_BY)) {
      props.setProperty(CREATED_BY, user)
    }

    if (!props.containsKey(GROUP)) {
      props.setProperty(GROUP, project.getGroupId)
    }

    if (!props.containsKey(NAME)) {
      props.setProperty(NAME, project.getArtifactId)
    }

    if (!props.containsKey(VERSION)) {
      props.setProperty(VERSION, project.getVersion)
    }

    if (!props.containsKey(DESCRIPTION) && project.getDescription != null) {
      props.setProperty(DESCRIPTION, project.getDescription)
    }

    if (!props.containsKey(DEPENDENCIES) && !packageDependencies.isEmpty) {
      props.setProperty(DEPENDENCIES, Dependency.toString(dependsOn: _*))
    }

    getLog.info("generating " + propertiesXml)

    val propertiesResource = Resource.fromFile(propertiesXml)
    propertiesResource.truncate(0)

    propertiesResource.outputStream.acquireFor {
      (f) => props.storeToXML(f, "generated by " + signature)
    } match {
      case Left(t :: ts) => throw t
      case _ => ()
    }
  }

  def generateConfigXml() {
    val file = new File(vaultSource, "config.xml")

    getLog.info("generating " + configXml)
    if (file.exists()) {
      Resource.fromFile(file).copyDataTo(Resource.fromFile(configXml))
    } else {
      Resource.fromClasspath(DEFAULT_CONFIG).copyDataTo(Resource.fromFile(configXml))
    }
  }

  def generateDefinition() {
    lazy val repository = new TransientRepository(transientRepoDirectory)
    lazy val session = repository.login(new SimpleCredentials("admin", "admin".toCharArray))
    try {
      val fakePack = getFakePackage
      val id = fakePack.getId
      val mgr = new JcrPackageManagerImpl(session)

      Option(mgr.getPackageRoot(true)) foreach {
        (node: Node) => {
          node.remove()
          session.save()
        }}

      val defPack = mgr.create(id.getGroup, id.getName, id.getVersionString)
      defPack.getDefinition.unwrap(fakePack, true, true)

      val defNode = defPack.getDefNode

      definitionProperties.foreach {
        p: (String, String) => {
          val (key, value) = p
          value match {
            case "true" => defNode.setProperty(key, true)
            case "false" => defNode.setProperty(key, false)
            case _ => defNode.setProperty(key, value)
          }
        }
      }

      defNode.setProperty("builtWith", signature)
      session.save()

      Option(thumbnail) foreach {
        (thumb) => {
          val tNode = defNode.addNode("thumbnail.png", JcrConstants.NT_FILE)
          val tResource = tNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE)
          tResource.setProperty(JcrConstants.JCR_MIMETYPE, "image/png")
          Resource.fromFile(thumbnail).inputStream.acquireFor {
            (in) => tResource.setProperty(JcrConstants.JCR_DATA, session.getValueFactory.createBinary(in))
          }
          session.save()
        }
      }

      screenshots.toList match {
        case Nil => ()
        case screens => {
          val digester = MessageDigest.getInstance("MD5")
          val parent = defNode.addNode("screenshots", JcrConstants.NT_UNSTRUCTURED)
          screens.filter { (f) => f.exists() && f.length() > 0 }.foreach {
            (file) => {
              digester.reset()
              val temp = parent.addNode("temp", JcrConstants.NT_UNSTRUCTURED)
              val sNode = temp.addNode("file", JcrConstants.NT_FILE)
              val sResource = sNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE)
              sResource.setProperty(JcrConstants.JCR_MIMETYPE, "image/png")
              Resource.fromFile(file).inputStream.acquireFor {
                (stream) => {
                  val ds = new DigestInputStream(stream, digester)
                  sResource.setProperty(JcrConstants.JCR_DATA, session.getValueFactory.createBinary(ds))
                  session.move(temp.getPath, parent.getPath + "/" + digester.digest.map("%02X" format _).mkString)
                  session.save()
                }
              }
            }
          }
        }
      }

      val postProcessor = defPack.getDefinition.asInstanceOf[JcrPackageDefinitionImpl].getInjectProcessor

      val exporter = new PlatformExporter(vaultInfDirectory)
      postProcessor.process(exporter)
    } finally {
      session.logout()
      repository.shutdown()
    }
  }

  def getFakePackage: VaultPackage = {
    val fakeMetaInf = new DefaultMetaInf
    Resource.fromFile(configXml).inputStream.acquireFor { fakeMetaInf.loadConfig(_, configXml.getPath) }
    Resource.fromFile(filterXml).inputStream.acquireFor { fakeMetaInf.loadFilter(_, filterXml.getPath) }
    Resource.fromFile(propertiesXml).inputStream.acquireFor { fakeMetaInf.loadProperties(_, propertiesXml.getPath) }

    new VaultPackage {
      def getMetaInf = fakeMetaInf
      def requiresRoot() = false
      def getCreatedBy = ""
      def getSize = 0L
      def getId = getIdFromProperties(fakeMetaInf.getProperties, None).get
      def getArchive = null
      def getLastWrappedBy = ""
      def getDescription = ""
      def getLastModified = null
      def getACHandling = null
      def getDependencies = null
      def getCreated = null
      def close() {}
      def isValid = false
      def isClosed = true
      def extract(p1: Session, p2: io.ImportOptions) {}
      def extract(p1: Session, p2: ImportOptions) {}
      def getLastModifiedBy = ""
      def getLastWrapped = null
      def getFile = vaultInfDirectory
    }
  }
}