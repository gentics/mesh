package com.gentics.mesh.graphdb;

import static com.gentics.mesh.MeshEnv.CONFIG_FOLDERNAME;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.metric.Metrics.TX_RETRY;
import static com.gentics.mesh.metric.Metrics.TX_TIME;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Timer;
import com.gentics.mesh.changelog.Change;
import com.gentics.mesh.changelog.changes.ChangesList;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.rest.admin.cluster.ClusterInstanceInfo;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.index.OrientDBIndexHandler;
import com.gentics.mesh.graphdb.index.OrientDBTypeHandler;
import com.gentics.mesh.graphdb.model.MeshElement;
import com.gentics.mesh.graphdb.spi.AbstractDatabase;
import com.gentics.mesh.graphdb.tx.OrientStorage;
import com.gentics.mesh.graphdb.tx.impl.OrientLocalStorageImpl;
import com.gentics.mesh.graphdb.tx.impl.OrientServerStorageImpl;
import com.gentics.mesh.metric.Metrics;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.util.DateUtils;
import com.gentics.mesh.util.ETag;
import com.gentics.mesh.util.PropertyUtil;
import com.hazelcast.core.HazelcastInstance;
import com.orientechnologies.common.concur.ONeedRetryException;
import com.orientechnologies.orient.core.OConstants;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.exception.OSchemaException;
import com.orientechnologies.orient.core.intent.OIntentMassiveInsert;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;
import com.orientechnologies.orient.server.hazelcast.OHazelcastPlugin;
import com.orientechnologies.orient.server.plugin.OServerPluginManager;
import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.FramedGraph;
import com.syncleus.ferma.ext.orientdb.DelegatingFramedOrientGraph;
import com.syncleus.ferma.ext.orientdb3.OrientDBTx;
import com.syncleus.ferma.tx.Tx;
import com.syncleus.ferma.tx.TxAction;
import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Graph;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;
import com.tinkerpop.blueprints.impls.orient.OrientElement;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientGraphNoTx;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedVertex;
import com.tinkerpop.pipes.util.FastNoSuchElementException;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * OrientDB specific mesh graph database implementation.
 */
@Singleton
public class OrientDBDatabase extends AbstractDatabase {

	private static final String ORIENTDB_SERVER_CONFIG = "orientdb-server-config.xml";

	private static final String ORIENTDB_BACKUP_CONFIG = "automatic-backup.json";

	private static final String ORIENTDB_SECURITY_SERVER_CONFIG = "security.json";

	private static final String ORIENTDB_DISTRIBUTED_CONFIG = "default-distributed-db-config.json";

	private static final String ORIENTDB_HAZELCAST_CONFIG = "hazelcast.xml";

	private static final String ORIENTDB_PLUGIN_FOLDERNAME = "orientdb-plugins";

	private static final Logger log = LoggerFactory.getLogger(OrientDBDatabase.class);

	private static final String ORIENTDB_STUDIO_ZIP = "orientdb-studio-3.0.19.zip";

	private TopologyEventBridge topologyEventBridge;

	private TypeResolver resolver;

	private OServer server;

	private OHazelcastPlugin hazelcastPlugin;

	private int maxRetry = 10;

	private OrientStorage txProvider;

	private MetricsService metrics;

	private Timer txTimer;

	private Counter txRetryCounter;

	private OrientDBIndexHandler indexHandler;

	private OrientDBTypeHandler typeHandler;

	@Inject
	public OrientDBDatabase(MetricsService metrics, OrientDBTypeHandler typeHandler, OrientDBIndexHandler indexHandler) {
		this.metrics = metrics;
		if (metrics != null) {
			txTimer = metrics.timer(TX_TIME);
			txRetryCounter = metrics.counter(TX_RETRY);
		}
		this.typeHandler = typeHandler;
		this.indexHandler = indexHandler;
	}

	@Override
	public void stop() {
		// // TODO let other nodes know we are stopping the instance?
		// if (options.getClusterOptions().isEnabled()) {
		// Mesh.vertx().eventBus().publish(MeshEvent.CLUSTER_NODE_LEAVING, new JsonObject().put("node", getNodeName()));
		// try {
		// Thread.sleep(2000);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		if (txProvider != null) {
			txProvider.close();
		}
		if (server != null) {
			server.shutdown();
		}
		Tx.setActive(null);
	}

	@Override
	public void clear() {
		txProvider.clear();
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

		OGlobalConfiguration.RID_BAG_EMBEDDED_TO_SBTREEBONSAI_THRESHOLD.setValue(Integer.MAX_VALUE);

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
		txProvider.setMassInsertIntent();
	}

	@Override
	public void resetIntent() {
		txProvider.resetIntent();
	}

	@Override
	public OrientGraph rawTx() {
		return txProvider.rawTx();
	}

	protected OrientGraphNoTx rawNoTx() {
		return txProvider.rawNoTx();
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
		if (server != null && server.isActive()) {
			txProvider = new OrientServerStorageImpl(options, server.getContext(), metrics);
		} else {
			txProvider = new OrientLocalStorageImpl(options, metrics);
		}
		// Open the storage
		txProvider.open();
	}

	@Override
	public void closeConnectionPool() {
		txProvider.close();
	}

	@Override
	public void shutdown() {
		Orient.instance().shutdown();
	}

	/**
	 * Create the needed configuration files in the filesystem if they can't be located.
	 * 
	 * @throws IOException
	 */
	@Override
	public void initConfigurationFiles() throws IOException {

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
		String pluginDir = Matcher.quoteReplacement(new File(ORIENTDB_PLUGIN_FOLDERNAME).getAbsolutePath());
		System.setProperty("ORIENTDB_PLUGIN_DIR", pluginDir);
		System.setProperty("plugin.directory", pluginDir);
		// configString = configString.replaceAll("%CONSOLE_LOG_LEVEL%", "info");
		// configString = configString.replaceAll("%FILE_LOG_LEVEL%", "info");
		System.setProperty("ORIENTDB_CONFDIR_NAME", CONFIG_FOLDERNAME);
		System.setProperty("ORIENTDB_NODE_NAME", getNodeName());
		System.setProperty("ORIENTDB_DISTRIBUTED", String.valueOf(options.getClusterOptions().isEnabled()));
		String networkHost = options.getClusterOptions().getNetworkHost();
		if (isEmpty(networkHost)) {
			networkHost = "0.0.0.0";
		}

		System.setProperty("ORIENTDB_NETWORK_HOST", networkHost);
		// Only use the cluster network host if clustering is enabled.
		String dbDir = storageOptions().getDirectory();
		if (dbDir != null) {
			System.setProperty("ORIENTDB_DB_PATH", escapeSafe(storageOptions().getDirectory()));
		} else {
			if (log.isDebugEnabled()) {
				log.debug("Not setting ORIENTDB_DB_PATH because no database dir was configured.");
			}
		}
		configString = PropertyUtil.resolve(configString);
		if (log.isDebugEnabled()) {
			log.debug("OrientDB server configuration:" + configString);
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
	public void registerEventHandlers() {
		// Mesh.vertx().eventBus().consumer(MeshEvent.CLUSTER_NODE_LEAVING, (Message<JsonObject> rh) -> {
		// String nodeName = rh.body().getString("node");
		// log.info("Received {" + MeshEvent.CLUSTER_NODE_LEAVING + "} for node {" + nodeName + "}. Removing node from config.");
		// removeNode(nodeName);
		// });
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
		try (InputStream ins = getClass().getResourceAsStream("/plugins/" + ORIENTDB_STUDIO_ZIP)) {
			File pluginDirectory = new File(ORIENTDB_PLUGIN_FOLDERNAME);
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
						if (!plugin.delete()) {
							log.error("Could not delete old plugin {" + plugin + "}");
						}
					}
				}
			}

			if (!currentPluginFound) {
				log.info("Extracting OrientDB Studio");
				IOUtils.copy(ins, new FileOutputStream(new File(pluginDirectory, ORIENTDB_STUDIO_ZIP)));
			}
		}
	}

	private void postStartupDBEventHandling() {
		// Get the database status
		DB_STATUS status = server.getDistributedManager().getDatabaseStatus(getNodeName(), "storage");
		// Pass it along to the topology event bridge
		topologyEventBridge.onDatabaseChangeStatus(getNodeName(), "storage", status);
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
	public OrientBaseGraph unwrapCurrentGraph() {
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
	public <T extends EdgeFrame> T findEdge(String fieldKey, Object fieldValue, Class<T> clazz) {
		FramedGraph graph = Tx.getActive().getGraph();
		OrientBaseGraph orientBaseGraph = unwrapCurrentGraph();
		Iterator<Edge> it = orientBaseGraph.getEdges(fieldKey, fieldValue).iterator();
		if (it.hasNext()) {
			return graph.frameNewElementExplicit(it.next(), clazz);
		}
		return null;
	}

	@Override
	public void reload(MeshElement element) {
		reload(element.getElement());
	}

	@Override
	public void reload(Element element) {
		if (element instanceof OrientElement) {
			if (metrics.isEnabled()) {
				metrics.meter(Metrics.GRAPH_ELEMENT_RELOAD).mark();
			}
			((OrientElement) element).reload();
		}
	}

	/**
	 * @deprecated Don't use tx method directly. Use {@link #tx(com.syncleus.ferma.tx.TxAction0)} instead to avoid tx commit issues.
	 */
	@Override
	@Deprecated
	public Tx tx() {
		return new OrientDBTx(txProvider, resolver);
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
			final Timer.Context context = txTimer.time();
			try (Tx tx = tx()) {
				handlerResult = txHandler.handle(tx);
				handlerFinished = true;
				tx.success();
			} catch (OSchemaException e) {
				log.error("OrientDB schema exception detected.");
				// TODO maybe we should invoke a metadata getschema reload?
				// factory.getTx().getRawGraph().getMetadata().getSchema().reload();
				// Database.getThreadLocalGraph().getMetadata().getSchema().reload();
			} catch (ONeedRetryException | FastNoSuchElementException e) {
				if (log.isTraceEnabled()) {
					log.trace("Error while handling transaction. Retrying " + retry, e);
				}
				int rnd = (int) (Math.random() * 6000.0);
				try {
					// Increase the delay for each retry by 25ms to give the other transaction a chance to finish
					Thread.sleep(50 + (retry * 25) + rnd);
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
			} finally {
				context.stop();
			}
			if (!handlerFinished && log.isDebugEnabled()) {
				log.debug("Retrying .. {" + retry + "}");
				if (metrics.isEnabled()) {
					txRetryCounter.inc();
				}
			}
			if (handlerFinished) {
				return handlerResult;
			}
		}
		throw new RuntimeException("Retry limit {" + maxRetry + "} for trx exceeded");
	}

	@Override
	public void backupGraph(String backupDirectory) throws IOException {
		txProvider.backup(backupDirectory);
	}

	@Override
	public void restoreGraph(String backupFile) throws IOException {
		txProvider.restore(backupFile);
	}

	@Override
	public void exportGraph(String outputDirectory) throws IOException {
		txProvider.exportGraph(outputDirectory);
	}

	@Override
	public void importGraph(String importFile) throws IOException {
		txProvider.importGraph(importFile);
	}

	@Override
	public String getElementVersion(Element element) {
		if (element instanceof WrappedVertex) {
			element = ((WrappedVertex) element).getBaseElement();
		}
		OrientElement e = (OrientElement) element;
		String uuid = element.getProperty("uuid");
		return ETag.hash(uuid + e.getRecord().getVersion());
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

	// /**
	// * Removes the server/node from the distributed configuration.
	// *
	// * @param iNode
	// */
	// public void removeNode(String iNode) {
	// log.info(
	// "Removing server {" + iNode + "} from distributed configuration on server {" + getNodeName() + "} in cluster {" + getClusterName() + "}");
	// server.getDistributedManager().removeServer(iNode, true);
	// }

	@Override
	public OrientDBTypeHandler type() {
		return typeHandler;
	}

	@Override
	public OrientDBIndexHandler index() {
		return indexHandler;
	}

	public OrientStorage getTxProvider() {
		return txProvider;
	}

}
