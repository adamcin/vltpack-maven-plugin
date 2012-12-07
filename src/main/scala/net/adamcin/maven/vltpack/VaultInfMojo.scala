package net.adamcin.maven.vltpack

import org.apache.maven.plugins.annotations.{Parameter, Mojo, LifecyclePhase}
import org.apache.maven.plugin.logging.Log
import java.io.File
import scalax.io.Resource
import java.util.{Calendar, Properties, Collections}
import com.day.jcr.vault.packaging.{ImportOptions, VaultPackage, JcrPackageDefinition, PackageId}
import com.day.jcr.vault.fs.config.{MetaInf, DefaultMetaInf, DefaultWorkspaceFilter}
import com.day.jcr.vault.fs.api.PathFilterSet
import com.day.jcr.vault.fs.filter.DefaultPathFilter
import org.apache.jackrabbit.util.ISO8601
import javax.jcr.{SimpleCredentials, Session}
import org.apache.jackrabbit.core.TransientRepository
import com.day.jcr.vault.packaging.impl.{JcrPackageDefinitionImpl, JcrPackageManagerImpl}
import com.day.jcr.vault.fs.io
import io.PlatformExporter

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
  with IdentifiesPackages {

  final val DEFAULT_VAULT_SOURCE = "${project.build.outputDirectory}/META-INF/vault"
  final val DEFAULT_CONFIG = "com/day/jcr/vault/fs/config/defaultConfig-1.1.xml"

  @Parameter(defaultValue = DEFAULT_VAULT_SOURCE)
  val vaultSource: File = null

  @Parameter
  val properties = Collections.emptyMap[String, String]

  @Parameter
  val thumbnail: File = null

  @Parameter
  val createDefinition = false

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
      addCloseAction(IOUtil.inputCloser)
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
    Resource.fromInputStream(filter.getSource).addCloseAction(IOUtil.inputCloser).
      copyDataTo(filterResource)
  }

  def generatePropertiesXml() {
    val props = new Properties()

    props.putAll(properties)

    props.setProperty(MetaInf.PACKAGE_FORMAT_VERSION, MetaInf.FORMAT_VERSION_2.toString)

    if (!props.containsKey(MetaInf.CREATED)) {
      props.setProperty(MetaInf.CREATED, ISO8601.format(Calendar.getInstance()))
    }

    if (!props.containsKey(MetaInf.CREATED_BY)) {
      props.setProperty(MetaInf.CREATED_BY, user)
    }

    if (!props.containsKey(JcrPackageDefinition.PN_GROUP)) {
      props.setProperty(JcrPackageDefinition.PN_GROUP, project.getGroupId)
    }

    if (!props.containsKey(JcrPackageDefinition.PN_NAME)) {
      props.setProperty(JcrPackageDefinition.PN_NAME, project.getArtifactId)
    }

    if (!props.containsKey(JcrPackageDefinition.PN_VERSION)) {
      props.setProperty(JcrPackageDefinition.PN_VERSION, project.getVersion)
    }

    if (!props.containsKey("description") && project.getDescription != null) {
      props.setProperty("description", project.getDescription)
    }

    val id = new PackageId(
      props.getProperty(JcrPackageDefinition.PN_GROUP),
      props.getProperty(JcrPackageDefinition.PN_NAME),
      props.getProperty(JcrPackageDefinition.PN_VERSION)
    )

    getLog.info("generating " + propertiesXml)
    val propertiesResource = Resource.fromFile(propertiesXml)
    propertiesResource.truncate(0)
    propertiesResource.outputStream.acquireFor {
      (f) => props.storeToXML(f, "generated by vltpack-maven-plugin")
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