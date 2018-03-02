package com.gentics.mesh.graphdb;

import static com.gentics.mesh.MeshEnv.CONFIG_FOLDERNAME;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.gentics.mesh.changelog.Change;
import com.gentics.mesh.changelog.changes.ChangesList;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.admin.cluster.ClusterInstanceInfo;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.AbstractDatabase;
import com.gentics.mesh.graphdb.spi.FieldType;
import com.gentics.mesh.util.DateUtils;
import com.gentics.mesh.util.ETag;
import com.hazelcast.core.HazelcastInstance;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.OConstants;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.command.OCommandOutputListener;
import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.db.tool.ODatabaseExport;
import com.orientechnologies.orient.core.db.tool.ODatabaseImport;
import com.orientechnologies.orient.core.exception.OSchemaException;
import com.orientechnologies.orient.core.index.OCompositeKey;
import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexCursor;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.intent.OIntentNoCache;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;
import com.orientechnologies.orient.server.hazelcast.OHazelcastPlugin;
import com.orientechnologies.orient.server.plugin.OServerPluginManager;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.ext.orientdb.DelegatingFramedOrientGraph;
import com.syncleus.ferma.ext.orientdb.OrientDBTx;
import com.syncleus.ferma.tx.Tx;
import com.syncleus.ferma.tx.TxAction;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientEdgeType;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import com.tinkerpop.blueprints.impls.orient.OrientVertexType;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * OrientDB specific mesh graph database implementation.
 */
public class OrientDBDatabase extends AbstractDatabase {

	private static final String ORIENTDB_SERVER_CONFIG = "orientdb-server-config.xml";

	private static final String ORIENTDB_BACKUP_CONFIG = "automatic-backup.json";

	private static final String ORIENTDB_SECURITY_SERVER_CONFIG = "security.json";

	private static final String ORIENTDB_DISTRIBUTED_CONFIG = "default-distributed-db-config.json";

	private static final String ORIENTDB_HAZELCAST_CONFIG = "hazelcast.xml";

	private static final Logger log = LoggerFactory.getLogger(OrientDBDatabase.class);

	private static final String DB_NAME = "storage";

	private static final String ORIENTDB_STUDIO_ZIP = "orientdb-studio-2.2.31.zip";

	private TopologyEventBridge topologyEventBridge;

	private OrientGraphFactory factory;

	private TypeResolver resolver;

	private OServer server;

	private OHazelcastPlugin hazelcastPlugin;

	private DateFormat formatter = new SimpleDateFormat("dd-MM-yyyy_HH-mm-ss-SSS");

	private int maxRetry = 100;

	@Override
	public void stop() {
		if (factory != null) {
			factory.close();
			Orient.instance().shutdown();
		}
		if (server != null) {
			server.shutdown();
		}
		Tx.setActive(null);
	}

	@Override
	public void clear() {
		if (log.isDebugEnabled()) {
			log.debug("Clearing graph");
		}

		OrientGraphNoTx tx2 = factory.getNoTx();
		tx2.declareIntent(new OIntentNoCache());
		try {
			for (Vertex vertex : tx2.getVertices()) {
				vertex.remove();
			}
		} finally {
			tx2.declareIntent(null);
			tx2.shutdown();
		}
		if (log.isDebugEnabled()) {
			log.debug("Cleared graph");
		}

	}

	@Override
	public void init(MeshOptions options, String meshVersion, String... basePaths) throws Exception {
		super.init(options, meshVersion);

		GraphStorageOptions storageOptions = options.getStorageOptions();
		boolean startOrientServer = storageOptions != null && storageOptions.getStartServer();
		boolean isInMemory = storageOptions.getDirectory() == null;

		if (isInMemory && startOrientServer) {
			throw new RuntimeException(
				"Using the graph database server is only possible for non-in-memory databases. You have not specified a graph database directory.");
		}

		initConfigurationFiles();

		// resolver = new OrientDBTypeResolver(basePaths);
		resolver = new MeshTypeResolver(basePaths);
		if (options != null && storageOptions.getParameters() != null && storageOptions.getParameters().get("maxTransactionRetry") != null) {
			this.maxRetry = Integer.valueOf(storageOptions.getParameters().get("maxTransactionRetry"));
			log.info("Using {" + this.maxRetry + "} transaction retries before failing");
		}
	}

	@Override
	public void setMassInsertIntent() {
		factory.declareIntent(new OIntentMassiveInsert());
	}

	@Override
	public void resetIntent() {
		factory.declareIntent(null);
	}

	@Override
	public TransactionalGraph rawTx() {
		return factory.getTx();
	}

	@Override
	public void joinCluster() throws InterruptedException {
		// Wait until another node joined the cluster
		int timeout = 500;
		log.info("Waiting {" + timeout + "} seconds for other nodes in the cluster.");
		if (!topologyEventBridge.waitForMainGraphDB(timeout, SECONDS)) {
			throw new RuntimeException("Waiting for cluster database source timed out after {" + timeout + "} seconds.");
		}
	}

	/**
	 * Start the orientdb related process. This will also setup the graph connection pool and handle clustering.
	 */
	@Override
	public void setupConnectionPool() throws Exception {
		Orient.instance().startup();
		initGraphDB();
	}

	/**
	 * Setup the OrientDB Graph connection
	 */
	private void initGraphDB() {
		GraphStorageOptions storageOptions = options.getStorageOptions();
		if (storageOptions == null || storageOptions.getDirectory() == null) {
			log.info("No graph database settings found. Fallback to in memory mode.");
			factory = new OrientGraphFactory("memory:tinkerpop").setupPool(16, 100);
		} else {
			factory = new OrientGraphFactory("plocal:" + new File(storageOptions.getDirectory(), DB_NAME).getAbsolutePath()).setupPool(16, 100);
		}
	}

	@Override
	public void closeConnectionPool() {
		factory.close();
		Orient.instance().shutdown();
	}

	/**
	 * Create the needed configuration files in the filesystem if they can't be located.
	 * 
	 * @throws IOException
	 */
	private void initConfigurationFiles() throws IOException {

		File distributedConfigFile = new File(CONFIG_FOLDERNAME + "/" + ORIENTDB_DISTRIBUTED_CONFIG);
		if (!distributedConfigFile.exists()) {
			log.info("Creating orientdb distributed server configuration file {" + distributedConfigFile + "}");
			writeDistributedConfig(distributedConfigFile);
		}

		File hazelcastConfigFile = new File(CONFIG_FOLDERNAME + "/" + ORIENTDB_HAZELCAST_CONFIG);
		if (!hazelcastConfigFile.exists()) {
			log.info("Creating orientdb hazelcast configuration file {" + hazelcastConfigFile + "}");
			writeHazelcastConfig(hazelcastConfigFile);
		}

		File serverConfigFile = new File(CONFIG_FOLDERNAME + "/" + ORIENTDB_SERVER_CONFIG);
		// Check whether the initial configuration needs to be written
		if (!serverConfigFile.exists()) {
			log.info("Creating orientdb server configuration file {" + serverConfigFile + "}");
			writeOrientServerConfig(serverConfigFile);
		}

		File backupConfigFile = new File(CONFIG_FOLDERNAME + "/" + ORIENTDB_BACKUP_CONFIG);
		// Check whether the initial configuration needs to be written
		if (!backupConfigFile.exists()) {
			log.info("Creating orientdb backup configuration file {" + backupConfigFile + "}");
			writeOrientBackupConfig(backupConfigFile);
		}

		File securityConfigFile = new File(CONFIG_FOLDERNAME + "/" + ORIENTDB_SECURITY_SERVER_CONFIG);
		// Check whether the initial configuration needs to be written
		if (!securityConfigFile.exists()) {
			log.info("Creating orientdb server security configuration file {" + securityConfigFile + "}");
			writeOrientServerSecurityConfig(securityConfigFile);
		}

	}

	private void writeOrientServerSecurityConfig(File securityConfigFile) throws IOException {
		String resourcePath = "/config/" + ORIENTDB_SECURITY_SERVER_CONFIG;
		InputStream configIns = getClass().getResourceAsStream(resourcePath);
		if (configIns == null) {
			log.error("Could not find default orientdb server security configuration file {" + resourcePath + "} within classpath.");
		}
		StringWriter writer = new StringWriter();
		IOUtils.copy(configIns, writer, StandardCharsets.UTF_8);
		String configString = writer.toString();
		FileUtils.writeStringToFile(securityConfigFile, configString);
	}

	private void writeHazelcastConfig(File hazelcastConfigFile) throws IOException {
		String resourcePath = "/config/" + ORIENTDB_HAZELCAST_CONFIG;
		InputStream configIns = getClass().getResourceAsStream(resourcePath);
		if (configIns == null) {
			log.error("Could not find default hazelcast configuration file {" + resourcePath + "} within classpath.");
		}
		StringWriter writer = new StringWriter();
		IOUtils.copy(configIns, writer, StandardCharsets.UTF_8);
		String configString = writer.toString();
		FileUtils.writeStringToFile(hazelcastConfigFile, configString);
	}

	private void writeDistributedConfig(File distributedConfigFile) throws IOException {
		String resourcePath = "/config/" + ORIENTDB_DISTRIBUTED_CONFIG;
		InputStream configIns = getClass().getResourceAsStream(resourcePath);
		if (configIns == null) {
			log.error("Could not find default distributed configuration file {" + resourcePath + "} within classpath.");
		}
		StringWriter writer = new StringWriter();
		IOUtils.copy(configIns, writer, StandardCharsets.UTF_8);
		String configString = writer.toString();
		FileUtils.writeStringToFile(distributedConfigFile, configString);
	}

	private String escapeSafe(String text) {
		return StringEscapeUtils.escapeJava(StringEscapeUtils.escapeXml11(new File(text).getAbsolutePath()));
	}

	private String getOrientServerConfig() throws Exception {
		File configFile = new File(CONFIG_FOLDERNAME + "/" + ORIENTDB_SERVER_CONFIG);
		String configString = FileUtils.readFileToString(configFile);

		// Now replace the parameters within the configuration
		configString = configString.replaceAll("%PLUGIN_DIRECTORY%", new File("orientdb-plugins").getAbsolutePath());
		configString = configString.replaceAll("%CONSOLE_LOG_LEVEL%", "info");
		configString = configString.replaceAll("%FILE_LOG_LEVEL%", "info");
		configString = configString.replaceAll("%CONFDIR_NAME%", CONFIG_FOLDERNAME);
		configString = configString.replaceAll("%NODENAME%", getNodeName());
		String networkHost = options.getClusterOptions().getNetworkHost();
		if (isEmpty(networkHost)) {
			networkHost = "0.0.0.0";
		}
		configString = configString.replaceAll("%NETWORK_HOST%", networkHost);
		configString = configString.replaceAll("%DISTRIBUTED%", String.valueOf(options.getClusterOptions().isEnabled()));
		// Only use the cluster network host if clustering is enabled.
		String dbDir = storageOptions().getDirectory();
		if (dbDir != null) {
			configString = configString.replaceAll("%DB_PARENT_PATH%", escapeSafe(storageOptions().getDirectory()));
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Not setting DB_PARENT_PATH because no database dir was configured.");
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("Effective orientdb server configuration:" + configString);
		}
		return configString;
	}

	/**
	 * Determine the OrientDB Node name.
	 * 
	 * @return
	 */
	public String getNodeName() {
		StringBuilder nameBuilder = new StringBuilder();
		String nodeName = options.getNodeName();
		nameBuilder.append(nodeName);
		nameBuilder.append("@");
		nameBuilder.append(meshVersion);

		// Sanitize the name
		String name = nameBuilder.toString();
		name = name.replaceAll(" ", "_");
		name = name.replaceAll("\\.", "-");
		return name;
	}

	private void writeOrientBackupConfig(File configFile) throws IOException {
		String resourcePath = "/config/automatic-backup.json";
		InputStream configIns = getClass().getResourceAsStream(resourcePath);
		if (configFile == null) {
			throw new RuntimeException("Could not find default orientdb backup configuration template file {" + resourcePath + "} within classpath.");
		}
		StringWriter writer = new StringWriter();
		IOUtils.copy(configIns, writer, StandardCharsets.UTF_8);
		String configString = writer.toString();
		FileUtils.writeStringToFile(configFile, configString);
	}

	private void writeOrientServerConfig(File configFile) throws IOException {
		String resourcePath = "/config/orientdb-server-config.xml";
		InputStream configIns = getClass().getResourceAsStream(resourcePath);
		if (configFile == null) {
			throw new RuntimeException("Could not find default orientdb server configuration template file {" + resourcePath + "} within classpath.");
		}
		StringWriter writer = new StringWriter();
		IOUtils.copy(configIns, writer, StandardCharsets.UTF_8);
		String configString = writer.toString();
		FileUtils.writeStringToFile(configFile, configString);
	}

	/**
	 * Start the OrientDB studio server by extracting the studio plugin zipfile.
	 * 
	 * @throws Exception
	 */
	@Override
	public void startServer() throws Exception {
		ClusterOptions clusterOptions = options.getClusterOptions();
		boolean isClusteringEnabled = clusterOptions != null && clusterOptions.isEnabled();
		String orientdbHome = new File("").getAbsolutePath();
		System.setProperty("ORIENTDB_HOME", orientdbHome);
		if (server == null) {
			server = OServerMain.create();
			updateOrientDBPlugin();
		}

		if (clusterOptions != null && clusterOptions.isEnabled()) {
			// This setting will be referenced by the hazelcast configuration
			System.setProperty("mesh.clusterName", clusterOptions.getClusterName() + "@" + getDatabaseRevision());
		}

		log.info("Starting OrientDB Server");
		server.startup(getOrientServerConfig());
		OServerPluginManager manager = new OServerPluginManager();
		manager.config(server);
		server.activate();
		if (isClusteringEnabled) {
			ODistributedServerManager distributedManager = server.getDistributedManager();
			topologyEventBridge = new TopologyEventBridge(this);
			distributedManager.registerLifecycleListener(topologyEventBridge);
			if (server.getDistributedManager() instanceof OHazelcastPlugin) {
				hazelcastPlugin = (OHazelcastPlugin) distributedManager;
			}
		}
		manager.startup();
		if (isClusteringEnabled) {
			// The registerLifecycleListener may not have been invoked. We need to redirect the online event manually.
			postStartupDBEventHandling();
		}
	}

	@Override
	public String getDatabaseRevision() {
		String overrideRev = System.getProperty("mesh.internal.dbrev");
		if (overrideRev != null) {
			return overrideRev;
		}
		StringBuilder builder = new StringBuilder();
		for (Change change : ChangesList.getList()) {
			builder.append(change.getUuid());
		}
		return ETag.hash(builder.toString() + getVersion());
	}

	/**
	 * Check the orientdb plugin directory and extract the orientdb studio plugin if needed.
	 * 
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private void updateOrientDBPlugin() throws FileNotFoundException, IOException {
		InputStream ins = getClass().getResourceAsStream("/plugins/" + ORIENTDB_STUDIO_ZIP);
		File pluginDirectory = new File("orientdb-plugins");
		pluginDirectory.mkdirs();

		// Remove old plugins
		boolean currentPluginFound = false;
		for (File plugin : pluginDirectory.listFiles()) {
			if (plugin.isFile()) {
				String filename = plugin.getName();
				log.debug("Checking orientdb plugin: " + filename);
				if (filename.equals(ORIENTDB_STUDIO_ZIP)) {
					currentPluginFound = true;
					continue;
				}
				if (filename.startsWith("orientdb-studio-")) {
					plugin.delete();
				}
			}
		}

		if (!currentPluginFound) {
			log.info("Extracting OrientDB Studio");
			IOUtils.copy(ins, new FileOutputStream(new File(pluginDirectory, "orientdb-studio-2.2.26.zip")));
		}

	}

	private void postStartupDBEventHandling() {
		// Get the database status
		DB_STATUS status = server.getDistributedManager().getDatabaseStatus(getNodeName(), "storage");
		// Pass it along to the topology event bridge
		topologyEventBridge.onDatabaseChangeStatus(getNodeName(), "storage", status);
	}

	@Override
	public void addCustomEdgeIndex(String label, String indexPostfix, String... fields) {
		OrientGraphNoTx noTx = factory.getNoTx();
		try {
			OrientEdgeType e = noTx.getEdgeType(label);
			if (e == null) {
				throw new RuntimeException("Could not find edge type {" + label + "}. Create edge type before creating indices.");
			}

			for (String key : fields) {
				if (e.getProperty(key) == null) {
					OType type = OType.STRING;
					if (key.equals("out") || key.equals("in")) {
						type = OType.LINK;
					}
					e.createProperty(key, type);
				}
			}
			String name = "e." + label + "_" + indexPostfix;
			name = name.toLowerCase();
			if (fields.length != 0 && e.getClassIndex(name) == null) {
				e.createIndex(name, OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX, fields);
			}

		} finally {
			noTx.shutdown();
		}
	}

	@Override
	public void addEdgeIndex(String label, boolean includeInOut, boolean includeIn, boolean includeOut, String... extraFields) {
		OrientGraphNoTx noTx = factory.getNoTx();
		try {
			OrientEdgeType e = noTx.getEdgeType(label);
			if (e == null) {
				e = noTx.createEdgeType(label);
			}
			if ((includeIn || includeInOut) && e.getProperty("in") == null) {
				e.createProperty("in", OType.LINK);
			}
			if ((includeOut || includeInOut) && e.getProperty("out") == null) {
				e.createProperty("out", OType.LINK);
			}
			for (String key : extraFields) {
				if (e.getProperty(key) == null) {
					e.createProperty(key, OType.STRING);
				}
			}
			String indexName = "e." + label.toLowerCase();
			String name = indexName + "_inout";
			if (includeInOut && e.getClassIndex(name) == null) {
				e.createIndex(name, OClass.INDEX_TYPE.NOTUNIQUE, new String[] { "in", "out" });
			}
			name = indexName + "_out";
			if (includeOut && e.getClassIndex(name) == null) {
				e.createIndex(name, OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX, new String[] { "out" });
			}
			name = indexName + "_in";
			if (includeIn && e.getClassIndex(name) == null) {
				e.createIndex(name, OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX, new String[] { "in" });
			}
			name = indexName + "_extra";
			if (extraFields.length != 0 && e.getClassIndex(name) == null) {
				e.createIndex(name, OClass.INDEX_TYPE.UNIQUE_HASH_INDEX, extraFields);
			}

		} finally {
			noTx.shutdown();
		}
	}

	@Override
	public List<Object> edgeLookup(String edgeLabel, String indexPostfix, Object key) {
		OrientBaseGraph orientBaseGraph = unwrapCurrentGraph();
		List<Object> ids = new ArrayList<>();

		// Load the edge type in order to access the indices of the edge
		OrientEdgeType edgeType = orientBaseGraph.getEdgeType(edgeLabel);
		if (edgeType != null) {
			// Fetch the required index
			OIndex<?> index = edgeType.getClassIndex("e." + edgeLabel.toLowerCase() + "_" + indexPostfix);
			if (index != null) {
				// Iterate over the sb-tree index entries
				OIndexCursor cursor = index.iterateEntriesMajor(new OCompositeKey(key), true, false);
				while (cursor.hasNext()) {
					Entry<Object, OIdentifiable> entry = cursor.nextEntry();
					if (entry != null) {
						OCompositeKey entryKey = (OCompositeKey) entry.getKey();
						// The index returns all entries. We thus need to filter it manually
						if (entryKey.getKeys().get(1).equals(key)) {
							Object inId = entryKey.getKeys().get(0);
							// Only add the inbound vertex id to the list of ids
							ids.add(inId);
						}
					} else {
						break;
					}
				}
			}
		}
		return ids;
	}

	@Override
	public Iterator<Vertex> getVertices(Class<?> classOfVertex, String[] fieldNames, Object[] fieldValues) {
		OrientBaseGraph orientBaseGraph = unwrapCurrentGraph();
		return orientBaseGraph.getVertices(classOfVertex.getSimpleName(), fieldNames, fieldValues).iterator();
	}

	@Override
	public <T extends MeshVertex> Iterator<? extends T> getVerticesForType(Class<T> classOfVertex) {
		OrientBaseGraph orientBaseGraph = unwrapCurrentGraph();
		FramedGraph fermaGraph = Tx.getActive().getGraph();
		Iterator<Vertex> rawIt = orientBaseGraph.getVertices("@class", classOfVertex.getSimpleName()).iterator();
		return fermaGraph.frameExplicit(rawIt, classOfVertex);
	}

	/**
	 * Unwrap the current thread local graph.
	 * 
	 * @return
	 */
	private OrientBaseGraph unwrapCurrentGraph() {
		FramedGraph graph = Tx.getActive().getGraph();
		Graph baseGraph = ((DelegatingFramedOrientGraph) graph).getBaseGraph();
		OrientBaseGraph tx = ((OrientBaseGraph) baseGraph);
		return tx;
	}

	@Override
	public void enableMassInsert() {
		OrientBaseGraph tx = unwrapCurrentGraph();
		tx.getRawGraph().getTransaction().setUsingLog(false);
		tx.declareIntent(new OIntentMassiveInsert().setDisableHooks(true).setDisableValidation(true));
	}

	@Override
	public void addEdgeType(String label, String... stringPropertyKeys) {
		addEdgeType(label, null, stringPropertyKeys);
	}

	@Override
	public void addEdgeType(String label, Class<?> superClazzOfEdge, String... stringPropertyKeys) {
		if (log.isDebugEnabled()) {
			log.debug("Adding edge type for label {" + label + "}");
		}
		OrientGraphNoTx noTx = factory.getNoTx();
		try {
			OrientEdgeType e = noTx.getEdgeType(label);
			if (e == null) {
				String superClazz = "E";
				if (superClazzOfEdge != null) {
					superClazz = superClazzOfEdge.getSimpleName();
				}
				e = noTx.createEdgeType(label, superClazz);
			} else {
				// Update the existing edge type and set the super class
				if (superClazzOfEdge != null) {
					OrientEdgeType superType = noTx.getEdgeType(superClazzOfEdge.getSimpleName());
					if (superType == null) {
						throw new RuntimeException("The supertype for edges with label {" + label + "} can't be set since the supertype {"
							+ superClazzOfEdge.getSimpleName() + "} was not yet added to orientdb.");
					}
					e.setSuperClass(superType);
				}
			}

			for (String key : stringPropertyKeys) {
				if (e.getProperty(key) == null) {
					e.createProperty(key, OType.STRING);
				}
			}
		} finally {
			noTx.shutdown();
		}
	}

	@Override
	public void addVertexType(Class<?> clazzOfVertex, Class<?> superClazzOfVertex) {
		if (log.isDebugEnabled()) {
			log.debug("Adding vertex type for class {" + clazzOfVertex.getName() + "}");
		}
		OrientGraphNoTx noTx = factory.getNoTx();
		try {
			OrientVertexType vertexType = noTx.getVertexType(clazzOfVertex.getSimpleName());
			if (vertexType == null) {
				String superClazz = "V";
				if (superClazzOfVertex != null) {
					superClazz = superClazzOfVertex.getSimpleName();
				}
				vertexType = noTx.createVertexType(clazzOfVertex.getSimpleName(), superClazz);
			} else {
				// Update the existing vertex type and set the super class
				if (superClazzOfVertex != null) {
					OrientVertexType superType = noTx.getVertexType(superClazzOfVertex.getSimpleName());
					if (superType == null) {
						throw new RuntimeException("The supertype for vertices of type {" + clazzOfVertex + "} can't be set since the supertype {"
							+ superClazzOfVertex.getSimpleName() + "} was not yet added to orientdb.");
					}
					vertexType.setSuperClass(superType);
				}
			}
		} finally {
			noTx.shutdown();
		}
	}

	@Override
	public void removeEdgeType(String typeName) {
		if (log.isDebugEnabled()) {
			log.debug("Removing vertex type with name {" + typeName + "}");
		}
		OrientGraphNoTx noTx = factory.getNoTx();
		try {
			noTx.dropEdgeType(typeName);
		} finally {
			noTx.shutdown();
		}
	}

	@Override
	public void removeVertexType(String typeName) {
		if (log.isDebugEnabled()) {
			log.debug("Removing vertex type with name {" + typeName + "}");
		}
		OrientGraphNoTx noTx = factory.getNoTx();
		try {
			OrientVertexType type = noTx.getVertexType(typeName);
			if (type != null) {
				noTx.dropVertexType(typeName);
			}
		} finally {
			noTx.shutdown();
		}
	}

	/**
	 * Convert the vendor agnostic type to an orientdb specific type.
	 * 
	 * @param fieldType
	 * @return
	 */
	private OType convertType(FieldType fieldType) {
		switch (fieldType) {
		case STRING:
			return OType.STRING;
		case INTEGER:
			return OType.INTEGER;
		case BOOLEAN:
			return OType.BOOLEAN;
		case STRING_SET:
			return OType.EMBEDDEDSET;
		case STRING_LIST:
			return OType.EMBEDDEDLIST;
		default:
			throw new RuntimeException("Unsupported type {" + fieldType + "}");
		}
	}

	/**
	 * Convert the vendor agnostic type to an orientdb specific sub type (eg. string for string lists)
	 * 
	 * @param fieldType
	 * @return
	 */
	private OType convertToSubType(FieldType fieldType) {
		switch (fieldType) {
		case STRING_SET:
			return OType.STRING;
		case STRING_LIST:
			return OType.STRING;
		default:
			return null;
		}
	}

	@Override
	public void addVertexIndex(String indexName, Class<?> clazzOfVertices, boolean unique, String fieldKey, FieldType fieldType) {
		if (log.isDebugEnabled()) {
			log.debug("Adding vertex index  for class {" + clazzOfVertices.getName() + "}");
		}
		OrientGraphNoTx noTx = factory.getNoTx();
		try {
			String name = clazzOfVertices.getSimpleName();
			OrientVertexType v = noTx.getVertexType(name);
			if (v == null) {
				throw new RuntimeException("Vertex type {" + name + "} is unknown. Can't create index {" + indexName + "}");
			}

			if (v.getProperty(fieldKey) == null) {
				OType type = convertType(fieldType);
				OType subType = convertToSubType(fieldType);
				if (subType != null) {
					v.createProperty(fieldKey, type, subType);
				} else {
					v.createProperty(fieldKey, type);
				}
			}

			if (v.getClassIndex(indexName) == null) {
				v.createIndex(indexName, unique ? OClass.INDEX_TYPE.UNIQUE_HASH_INDEX.toString() : OClass.INDEX_TYPE.NOTUNIQUE_HASH_INDEX.toString(),
					null, new ODocument().fields("ignoreNullValues", true), new String[] { fieldKey });
			}
		} finally {
			noTx.shutdown();
		}

	}

	@Override
	public <T extends MeshElement> T findVertex(String fieldKey, Object fieldValue, Class<T> clazz) {
		FramedGraph graph = Tx.getActive().getGraph();
		OrientBaseGraph orientBaseGraph = unwrapCurrentGraph();
		Iterator<Vertex> it = orientBaseGraph.getVertices(clazz.getSimpleName(), new String[] { fieldKey }, new Object[] { fieldValue }).iterator();
		if (it.hasNext()) {
			return graph.frameNewElementExplicit(it.next(), clazz);
		}
		return null;
	}

	@Override
	public <T extends MeshElement> T checkIndexUniqueness(String indexName, T element, Object key) {
		FramedGraph graph = Tx.getActive().getGraph();
		OrientBaseGraph orientBaseGraph = unwrapCurrentGraph();

		OrientVertexType vertexType = orientBaseGraph.getVertexType(element.getClass().getSimpleName());
		if (vertexType != null) {
			OIndex<?> index = vertexType.getClassIndex(indexName);
			if (index != null) {
				Object recordId = index.get(key);
				if (recordId != null) {
					if (recordId.equals(element.getElement().getId())) {
						return null;
					} else {
						return (T) graph.getFramedVertexExplicit(element.getClass(), recordId);
					}
				}
			}
		}
		return null;
	}

	@Override
	public <T extends MeshElement> T checkIndexUniqueness(String indexName, Class<T> classOfT, Object key) {
		FramedGraph graph = Tx.getActive().getGraph();
		Graph baseGraph = ((DelegatingFramedOrientGraph) graph).getBaseGraph();
		OrientBaseGraph orientBaseGraph = ((OrientBaseGraph) baseGraph);

		OrientVertexType vertexType = orientBaseGraph.getVertexType(classOfT.getSimpleName());
		if (vertexType != null) {
			OIndex<?> index = vertexType.getClassIndex(indexName);
			if (index != null) {
				Object recordId = index.get(key);
				if (recordId != null) {
					return (T) graph.getFramedVertexExplicit(classOfT, recordId);
				}
			}
		}
		return null;
	}

	@Override
	public void reload(MeshElement element) {
		((OrientVertex) element.getElement()).reload();
	}

	@Override
	public Tx tx() {
		return new OrientDBTx(factory, resolver);
	}

	@Override
	public <T> T tx(TxAction<T> txHandler) {
		/**
		 * OrientDB uses the MVCC pattern which requires a retry of the code that manipulates the graph in cases where for example an
		 * {@link OConcurrentModificationException} is thrown.
		 */
		T handlerResult = null;
		boolean handlerFinished = false;
		for (int retry = 0; retry < maxRetry; retry++) {

			try (Tx tx = tx()) {
				handlerResult = txHandler.handle(tx);
				handlerFinished = true;
				tx.success();
			} catch (OSchemaException e) {
				log.error("OrientDB schema exception detected.");
				// TODO maybe we should invoke a metadata getschema reload?
				// factory.getTx().getRawGraph().getMetadata().getSchema().reload();
				// Database.getThreadLocalGraph().getMetadata().getSchema().reload();
			} catch (ONeedRetryException e) {
				if (log.isTraceEnabled()) {
					log.trace("Error while handling transaction. Retrying " + retry, e);
				}
				try {
					// Increase the delay for each retry by 25ms to give the other transaction a chance to finish
					Thread.sleep(50 + (retry * 25));
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				// Reset previous result
				handlerFinished = false;
				handlerResult = null;
			} catch (ORecordDuplicatedException e) {
				log.error(e);
				throw error(INTERNAL_SERVER_ERROR, "error_internal");
			} catch (GenericRestException e) {
				// Don't log. Just throw it along so that others can handle it
				throw e;
			} catch (RuntimeException e) {
				if (log.isDebugEnabled()) {
					log.debug("Error handling transaction", e);
				}
				throw e;
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug("Error handling transaction", e);
				}
				throw new RuntimeException("Transaction error", e);
			}
			if (!handlerFinished && log.isDebugEnabled()) {
				log.debug("Retrying .. {" + retry + "}");
			}
			if (handlerFinished) {
				return handlerResult;
			}
		}
		throw new RuntimeException("Retry limit {" + maxRetry + "} for trx exceeded");
	}

	@Override
	public void backupGraph(String backupDirectory) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("Running backup to backup directory {" + backupDirectory + "}.");
		}
		ODatabaseDocumentTx db = factory.getDatabase();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};
			String dateString = formatter.format(new Date());
			String backupFile = "backup_" + dateString + ".zip";
			new File(backupDirectory).mkdirs();
			OutputStream out = new FileOutputStream(new File(backupDirectory, backupFile).getAbsolutePath());
			db.backup(out, null, null, listener, 9, 2048);
		} finally {
			db.close();
		}
	}

	@Override
	public void restoreGraph(String backupFile) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("Running restore using {" + backupFile + "} backup file.");
		}
		ODatabaseDocumentTx db = factory.getDatabase();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};
			InputStream in = new FileInputStream(backupFile);
			db.restore(in, null, null, listener);
		} finally {
			db.close();
		}
	}

	@Override
	public void exportGraph(String outputDirectory) throws IOException {
		if (log.isDebugEnabled()) {
			log.debug("Running export to {" + outputDirectory + "} directory.");
		}
		ODatabaseDocumentTx db = factory.getDatabase();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};

			String dateString = formatter.format(new Date());
			String exportFile = "export_" + dateString;
			new File(outputDirectory).mkdirs();
			ODatabaseExport export = new ODatabaseExport(db, new File(outputDirectory, exportFile).getAbsolutePath(), listener);
			export.exportDatabase();
			export.close();
		} finally {
			db.close();
		}
	}

	@Override
	public void importGraph(String importFile) throws IOException {
		ODatabaseDocumentTx db = factory.getDatabase();
		try {
			OCommandOutputListener listener = new OCommandOutputListener() {
				@Override
				public void onMessage(String iText) {
					System.out.println(iText);
				}
			};
			ODatabaseImport databaseImport = new ODatabaseImport(db, importFile, listener);
			databaseImport.importDatabase();
			databaseImport.close();
		} finally {
			db.close();
		}

	}

	@Override
	public Object createComposedIndexKey(Object... keys) {
		return new OCompositeKey(keys);
	}

	@Override
	public void setVertexType(Element element, Class<?> classOfVertex) {
		if (element instanceof WrappedVertex) {
			element = ((WrappedVertex) element).getBaseElement();
		}
		((OrientVertex) element).moveToClass(classOfVertex.getSimpleName());
	}

	@Override
	public String getVendorName() {
		return "orientdb";
	}

	@Override
	public String getVersion() {
		return OConstants.getVersion();
	}

	@Override
	public HazelcastInstance getHazelcast() {
		return hazelcastPlugin != null ? hazelcastPlugin.getHazelcastInstance() : null;
	}

	public ClusterStatusResponse getClusterStatus() {
		ClusterStatusResponse response = new ClusterStatusResponse();
		if (hazelcastPlugin != null) {
			ODocument distribCfg = hazelcastPlugin.getClusterConfiguration();

			Collection<ODocument> members = distribCfg.field("members");
			if (members != null) {
				for (ODocument m : members) {
					if (m == null) {
						continue;
					}
					ClusterInstanceInfo instanceInfo = new ClusterInstanceInfo();
					String name = m.field("name");

					int idx = name.indexOf("@");
					if (idx > 0) {
						name = name.substring(0, idx);
					}

					instanceInfo.setName(name);
					instanceInfo.setStatus(m.field("status"));
					Date date = m.field("startedOn");
					instanceInfo.setStartDate(DateUtils.toISO8601(date.getTime()));

					String address = null;
					Collection<Map> listeners = m.field("listeners");
					if (listeners != null) {
						for (Map l : listeners) {
							String protocol = (String) l.get("protocol");
							if (protocol.equals("ONetworkProtocolBinary")) {
								address = (String) l.get("listen");
							}
						}
					}
					instanceInfo.setAddress(address);

					response.getInstances().add(instanceInfo);
				}
			}
		}
		return response;
	}
}
