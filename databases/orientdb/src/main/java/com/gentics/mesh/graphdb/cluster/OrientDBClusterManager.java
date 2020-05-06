package com.gentics.mesh.graphdb.cluster;

import static com.gentics.mesh.MeshEnv.CONFIG_FOLDERNAME;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.MeshStatus;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.rest.admin.cluster.ClusterInstanceInfo;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.GraphStorageOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.OrientDBDatabase;
import com.gentics.mesh.graphdb.spi.GraphStorage;
import com.gentics.mesh.util.DateUtils;
import com.gentics.mesh.util.PropertyUtil;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.config.OServerConfiguration;
import com.orientechnologies.orient.server.config.OServerConfigurationManager;
import com.orientechnologies.orient.server.config.OServerHandlerConfiguration;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;
import com.orientechnologies.orient.server.hazelcast.OHazelcastPlugin;
import com.orientechnologies.orient.server.plugin.OServerPluginManager;

import dagger.Lazy;
import io.reactivex.Completable;
import io.vertx.core.Vertx;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class OrientDBClusterManager implements ClusterManager {

	private static final Logger log = LoggerFactory.getLogger(OrientDBClusterManager.class);

	private static final String ORIENTDB_PLUGIN_FOLDERNAME = "orientdb-plugins";

	private static final String ORIENTDB_STUDIO_ZIP = "orientdb-studio-3.0.29.zip";

	private static final String ORIENTDB_DISTRIBUTED_CONFIG = "default-distributed-db-config.json";

	private static final String ORIENTDB_SERVER_CONFIG = "orientdb-server-config.xml";

	private static final String ORIENTDB_BACKUP_CONFIG = "automatic-backup.json";

	private static final String ORIENTDB_SECURITY_SERVER_CONFIG = "security.json";

	private static final String ORIENTDB_HAZELCAST_CONFIG = "hazelcast.xml";

	private OServer server;

	private HazelcastInstance hazelcastInstance;

	private OHazelcastPlugin hazelcastPlugin;

	private TopologyEventBridge topologyEventBridge;

	private final Mesh mesh;

	private final Lazy<Vertx> vertx;

	private final MeshOptions options;

	private final Lazy<OrientDBDatabase> db;

	private final Lazy<BootstrapInitializer> boot;

	private final ClusterOptions clusterOptions;

	private final boolean isClusteringEnabled;

	@Inject
	public OrientDBClusterManager(Mesh mesh, Lazy<Vertx> vertx, Lazy<BootstrapInitializer> boot, MeshOptions options, Lazy<OrientDBDatabase> db) {
		this.mesh = mesh;
		this.vertx = vertx;
		this.boot = boot;
		this.options = options;
		this.db = db;
		this.clusterOptions = options.getClusterOptions();
		this.isClusteringEnabled = clusterOptions != null && clusterOptions.isEnabled();
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

		// Apply on-the-fly fix for changed OrientDB configuration.
		final String OLD_PLUGIN_REGEX = "com\\.orientechnologies\\.orient\\.server\\.hazelcast\\.OHazelcastPlugin";
		final String NEW_PLUGIN = "com.gentics.mesh.graphdb.cluster.MeshOHazelcastPlugin";
		configString = configString.replaceAll(OLD_PLUGIN_REGEX, NEW_PLUGIN);

		return configString;
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
	public HazelcastInstance getHazelcast() {
		return hazelcastInstance;
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
		String resourcePath = "/config/" + ORIENTDB_SERVER_CONFIG;
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

	public ClusterStatusResponse getClusterStatus() {
		ClusterStatusResponse response = new ClusterStatusResponse();
		if (hazelcastPlugin != null) {
			ODocument distribCfg = hazelcastPlugin.getClusterConfiguration();
			ODocument dbConfig = (ODocument) hazelcastPlugin.getConfigurationMap().get("database.storage");
			ODocument serverConfig = dbConfig.field("servers");

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
					instanceInfo.setRole(serverConfig.field(name));

					response.getInstances().add(instanceInfo);
				}
			}
		}
		return response;
	}

	/**
	 * Start the OrientDB studio server by extracting the studio plugin zipfile.
	 * 
	 * @throws Exception
	 */
	@Override
	public void start() throws Exception {

		String orientdbHome = new File("").getAbsolutePath();
		System.setProperty("ORIENTDB_HOME", orientdbHome);
		if (clusterOptions != null && clusterOptions.isEnabled()) {
			// This setting will be referenced by the hazelcast configuration
			System.setProperty("mesh.clusterName", clusterOptions.getClusterName() + "@" + db.get().getDatabaseRevision());
		}

		ByteArrayInputStream configXML = new ByteArrayInputStream(getOrientServerConfig().getBytes());
		OServerConfigurationManager serverConfigManager = new OServerConfigurationManager(configXML);
		OServerConfiguration serverConfig = serverConfigManager.getConfiguration();

		startHazelcast(serverConfig);
		ILock lock = null;
		long lockTimeout = clusterOptions.getTopologyLockTimeout();
		if (isClusteringEnabled) {
			lock = hazelcastInstance.getLock(WriteLock.GLOBAL_LOCK_KEY);
			Thread.sleep(4000);
		}
		if (server == null) {
			server = OServerMain.create();
			updateOrientDBPlugin();
		}

		log.info("Starting OrientDB Server");
		server.startup(getOrientServerConfig());

		try {
			if (lock != null) {
				boolean isTimeout = !lock.tryLock(lockTimeout, TimeUnit.MILLISECONDS);
				Thread.sleep(4000);
				if (isTimeout) {
					log.warn("The topology lock for the pending server startup reached the timeout limit.");
				}
				if (log.isDebugEnabled()) {
					log.debug("Locking global write lock due to server startup.");
				}
			}
			activateServer();
		} finally {
			if (lock != null) {
				if (log.isDebugEnabled()) {
					log.debug("Unlocking global write lock after server startup.");
				}
				Thread.sleep(8000);
				if (lock.isLockedByCurrentThread()) {
					lock.unlock();
				}
			}
		}
	}

	private void startHazelcast(OServerConfiguration serverConfig) throws FileNotFoundException {
		Optional<OServerHandlerConfiguration> hazelcastPluginConfigOpt = serverConfig.handlers.stream()
			.filter(e -> e.clazz.equals(MeshOHazelcastPlugin.class.getName())).findFirst();
		if (!hazelcastPluginConfigOpt.isPresent()) {
			throw new RuntimeException("Could not find hazelcast plugin configuration in orientdb configuration file");
		}
		OServerHandlerConfiguration hazelcastPluginConfig = hazelcastPluginConfigOpt.get();
		hazelcastInstance = MeshOHazelcastPlugin.createHazelcast(hazelcastPluginConfig.parameters);
	}

	private void activateServer() throws Exception {
		OServerPluginManager manager = new OServerPluginManager();
		manager.config(server);
		server.activate();

		if (isClusteringEnabled) {
			ODistributedServerManager distributedManager = server.getDistributedManager();
			if (server.getDistributedManager() instanceof OHazelcastPlugin) {
				hazelcastPlugin = (OHazelcastPlugin) distributedManager;
			}

			topologyEventBridge = new TopologyEventBridge(options, vertx, boot, this, getHazelcast());
			distributedManager.registerLifecycleListener(topologyEventBridge);
		}

		manager.startup();

		if (isClusteringEnabled) {
			// The registerLifecycleListener may not have been invoked. We need to redirect the online event manually.
			postStartupDBEventHandling();
			if (!options.isInitClusterMode()) {
				mesh.setStatus(MeshStatus.WAITING_FOR_CLUSTER);
				joinCluster();
				mesh.setStatus(MeshStatus.STARTING);
				// Add a safety margin
				Thread.sleep(options.getClusterOptions().getTopologyLockDelay());
			}
		}

	}

	/**
	 * Join the cluster and block until the graph database has been received.
	 * 
	 * @throws InterruptedException
	 */
	private void joinCluster() throws InterruptedException {
		// Wait until another node joined the cluster
		int timeout = 500;
		log.info("Waiting {" + timeout + "} seconds for other nodes in the cluster.");
		if (!topologyEventBridge.waitForMainGraphDB(timeout, SECONDS)) {
			throw new RuntimeException("Waiting for cluster database source timed out after {" + timeout + "} seconds.");
		}
	}

	@Override
	public void stop() {
		log.info("Stopping cluster manager");
		if (server != null) {
			log.info("Stopping OrientDB Server");
			server.shutdown();
		}
	}

	@Override
	public void stopHazelcast() {
		if (hazelcastInstance != null) {
			log.info("Stopping hazelcast");
			hazelcastInstance.shutdown();
		}
	}

	public boolean isServerActive() {
		return server != null && server.isActive();
	}

	public OServer getServer() {
		return server;
	}

	/**
	 * Return the graph database storage options.
	 * 
	 * @return
	 */
	public GraphStorageOptions storageOptions() {
		return options.getStorageOptions();
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

	public OHazelcastPlugin getHazelcastPlugin() {
		return hazelcastPlugin;
	}

	/**
	 * @see TopologyEventBridge#isClusterTopologyLocked()
	 * @return
	 */
	public boolean isClusterTopologyLocked() {
		if (topologyEventBridge == null) {
			return false;
		} else {
			return topologyEventBridge.isClusterTopologyLocked();
		}
	}

	@Override
	public Completable waitUntilWriteQuorumReached() {
		return Completable.defer(() -> {
			try {
				return new io.vertx.reactivex.core.Vertx(vertx.get())
					.periodicStream(1000)
					.toObservable()
					.map(ignore -> {
						System.out.println("Ping: " + ignore);
						try {
							// The server and manager may not yet be initialized. We need to wait until those are ready
							if (server != null && server.getDistributedManager() != null) {
								return server.getDistributedManager().isWriteQuorumPresent(GraphStorage.DB_NAME);
							} else {
								return false;
							}
						} catch (Throwable e) {
							log.error("Error while checking write quorum", e);
							return false;
						}
					})
					.takeUntil(ready -> ready)
					.ignoreElements();
			} catch (Throwable t) {
				t.printStackTrace();
				return Completable.complete();
			}
		});
	}

}
