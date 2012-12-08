package net.adamcin.maven.vltpack.mojo

import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}
import org.apache.maven.plugin.logging.Log
import java.io.File
import scalax.io.Resource
import java.util.{Calendar, Properties, Collections}
import com.day.jcr.vault.packaging._
import com.day.jcr.vault.fs.config.{MetaInf, DefaultMetaInf, DefaultWorkspaceFilter}
import com.day.jcr.vault.fs.api.PathFilterSet
import com.day.jcr.vault.fs.filter.DefaultPathFilter
import org.apache.jackrabbit.util.ISO8601
import javax.jcr.{SimpleCredentials, Session}
import org.apache.jackrabbit.core.TransientRepository
import com.day.jcr.vault.packaging.impl.{JcrPackageDefinitionImpl, JcrPackageManagerImpl}
import com.day.jcr.vault.fs.io
import io.PlatformExporter
import com.day.jcr.vault.util.JcrConstants
import collection.JavaConversions
import java.security.{DigestInputStream, MessageDigest}
import net.adamcin.maven.vltpack._
import scala.Some

/**
 *
 * @version $Id: VaultInfMojo.java$
 * @author madamcin
 */
@Mojo(
  name = "vault-inf",
  defaultPhase = LifecyclePhase.PREPARE_PACKAGE)
class VaultInfMojo
  extends BaseMojo
  with UsernameAware
  with OutputParameters
  with BundlePathParameters
  with PackageDependencies
  with IdentifiesPackages {

  final val DEFAULT_VAULT_SOURCE = "${project.build.outputDirectory}/META-INF/vault"
  final val DEFAULT_CONFIG = "com/day/jcr/vault/fs/config/defaultConfig-1.1.xml"

  @Parameter(defaultValue = DEFAULT_VAULT_SOURCE)
  val vaultSource: File = null

  @Parameter
  val properties = Collections.emptyMap[String, String]

  @Parameter
  val definitionProperties = Collections.emptyMap[String, String]

  @Parameter
  val createDefinition = false

  @Parameter
  val thumbnail: File = null

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

  override def printParams(log: Log) {
    super.printParams(log)
    log.info("vaultSource = " + vaultSource)
  }

  def generateFilterXml() {
    val file = new File(vaultSource, "filter.xml")

    val filter = new DefaultWorkspaceFilter

    if (file.exists()) {
      val sourceFilter = new DefaultWorkspaceFilter
      sourceFilter.load(file)
      filter.getFilterSets.addAll(sourceFilter.getFilterSets)
    }

    val embedBundles = embedBundlesDirectory.listFiles
    if (embedBundles.size > 0) {
      val bundleFilterSet =
        if (filter.covers(bundleInstallPath)) {
          filter.getCoveringFilterSet(bundleInstallPath)
        } else {
          val set = new PathFilterSet(bundleInstallPath)
          set.addExclude(new DefaultPathFilter(bundleInstallPath + "(/.*)?"))
          filter.getFilterSets.add(set)
          set
        }

      embedBundles.foreach {
        (bundle) => bundleFilterSet.addInclude(new DefaultPathFilter(bundleInstallPath + "/" + bundle.getName))
      }
    }

    val embedPackages = embedPackagesDirectory.listFiles
    if (embedPackages.size > 0) {
      val packageFilterSet =
        if (filter.covers(PackageId.ETC_PACKAGES)) {
          filter.getCoveringFilterSet(PackageId.ETC_PACKAGES)
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
      val defPack = mgr.create(id.getGroup, id.getName, id.getVersionString)
      defPack.getDefinition.unwrap(fakePack, true, true)

      val defNode = defPack.getDefNode

      JavaConversions.mapAsScalaMap(definitionProperties).foreach {
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

      Option(thumbnail) match {
        case None => ()
        case Some(thumb) => {
          val tNode = defNode.addNode("thumbnail.png", JcrConstants.NT_FILE)
          val tResource = tNode.addNode(JcrConstants.JCR_CONTENT, JcrConstants.NT_RESOURCE)
          tResource.setProperty(JcrConstants.JCR_MIMETYPE, "image/png")
          Resource.fromFile(thumbnail).inputStream.acquireFor {
            (in) => tResource.setProperty(JcrConstants.JCR_DATA, session.getValueFactory.createBinary(in))
          }
          session.save()
        }
      }

      JavaConversions.collectionAsScalaIterable(screenshots).toList match {
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
                  session.move(temp.getPath, parent + "/" + digester.digest.map("%02X" format _).mkString)
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