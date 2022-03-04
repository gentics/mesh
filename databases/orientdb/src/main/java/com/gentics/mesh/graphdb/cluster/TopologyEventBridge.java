package com.gentics.mesh.graphdb.cluster;

import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_DATABASE_CHANGE_STATUS;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_JOINED;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_JOINING;
import static com.gentics.mesh.core.rest.MeshEvent.CLUSTER_NODE_LEFT;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.hazelcast.core.HazelcastInstance;
import com.orientechnologies.orient.server.distributed.ODistributedLifecycleListener;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;

import dagger.Lazy;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Listener for OrientDB cluster specific events. The listener relays the events via messages to the eventbus.
 */
public class TopologyEventBridge implements ODistributedLifecycleListener {

	private static final Logger log = LoggerFactory.getLogger(TopologyEventBridge.class);

	public static final String DB_STATUS_MAP_KEY = "DB_STATUS_MAP";

	public static final String SERVER_STATUS_MAP_KEY = "SERVER_STATUS_MAP";

	private final Lazy<Vertx> vertx;

	private OrientDBClusterManagerImpl manager;

	private final Lazy<BootstrapInitializer> boot;

	/**
	 * Map containing the database stati (per cluster node).
	 * This is a local map, since the data is fetched from OrientDB's distributed manager
	 */
	private Map<String, DB_STATUS> databaseStatusMap;

	private CountDownLatch nodeJoinLatch = new CountDownLatch(1);

	private ClusterOptions clusterOptions;

	public TopologyEventBridge(MeshOptions options, Lazy<Vertx> vertx, Lazy<BootstrapInitializer> boot, OrientDBClusterManagerImpl manager,
		HazelcastInstance hz) {
		this.clusterOptions = options.getClusterOptions();
		this.vertx = vertx;
		this.boot = boot;
		this.manager = manager;
		this.databaseStatusMap = new HashMap<>();
	}

	EventBus getEventBus() {
		return vertx.get().eventBus();
	}

	@Override
	public boolean onNodeJoining(String nodeName) {
		// Set the db into sync since we want to prevent
		// the lock from being released in between db status changes
		// and server online status.
		// this entry will keep the topology locked until the node is known
		// to the distributed manager (and the db status is ONLINE)
		databaseStatusMap.put(nodeName, DB_STATUS.SYNCHRONIZING);

		if (log.isInfoEnabled()) {
			log.info("Node {" + nodeName + "} is joining the cluster.");
		}
		isClusterTopologyLocked();

		if (isVertxReady()) {
			getEventBus().publish(CLUSTER_NODE_JOINING.address, nodeName);
		}
		return true;
	}

	@Override
	public void onNodeJoined(String nodeName) {
		if (log.isInfoEnabled()) {
			log.info("Node {" + nodeName + "} joined the cluster.");
		}
		isClusterTopologyLocked();

		if (isVertxReady()) {
			getEventBus().publish(CLUSTER_NODE_JOINED.address, nodeName);
		}
	}

	@Override
	public void onNodeLeft(String nodeName) {
		databaseStatusMap.remove(nodeName);

		if (log.isInfoEnabled()) {
			log.info("Node {" + nodeName + "} left the cluster");
		}
		isClusterTopologyLocked();
		if (isVertxReady()) {
			getEventBus().publish(CLUSTER_NODE_LEFT.address, nodeName);
		}
	}

	@Override
	public void onDatabaseChangeStatus(String nodeName, String iDatabaseName, DB_STATUS iNewStatus) {
		if (iNewStatus == DB_STATUS.ONLINE) {
			// Delay the online status a few seconds
			long postOnlineDelay = clusterOptions.getTopologyLockDelay();
			if (postOnlineDelay != 0) {
				try {
					Thread.sleep(postOnlineDelay);
				} catch (InterruptedException e) {
					log.warn("Topology lock delay was interrupted", e);
				}
			}
		}
		databaseStatusMap.put(nodeName, iNewStatus);
		if (log.isInfoEnabled()) {
			log.info("Node {" + nodeName + "} Database {" + iDatabaseName + "} changed status {" + iNewStatus.name() + "}");
		}
		isClusterTopologyLocked();
		if (isVertxReady()) {
			JsonObject statusInfo = new JsonObject();
			statusInfo.put("node", nodeName);
			statusInfo.put("database", iDatabaseName);
			statusInfo.put("status", iNewStatus.name());
			statusInfo.put("online", iNewStatus == DB_STATUS.ONLINE);
			getEventBus().publish(CLUSTER_DATABASE_CHANGE_STATUS.address, statusInfo);
		}
		if ("storage".equals(iDatabaseName) && iNewStatus == DB_STATUS.ONLINE && nodeName.equals(manager.getNodeName())) {
			nodeJoinLatch.countDown();
		}
	}

	/**
	 * Block until another node joined the cluster and the database is ready to use.
	 * 
	 * @param timeout
	 *            the maximum time to wait
	 * @param unit
	 *            the time unit of the {@code timeout} argument
	 * @return {@code true} if the count reached zero and {@code false} if the waiting time elapsed before the count reached zero
	 * @throws InterruptedException
	 *             if the current thread is interrupted while waiting
	 */
	public boolean waitForMainGraphDB(int timeout, TimeUnit unit) throws InterruptedException {
		return nodeJoinLatch.await(timeout, unit);
	}

	public boolean isVertxReady() {
		return boot.get().isVertxReady();
	}

	/**
	 * Check whether a topology change in the database / cluster setup is requiring a lock.
	 * 
	 * Calling this method will synchronize the stati from the distributed manager.
	 * 
	 * @return true iff the database on at least one node is in status BACKUP or SYNCHRONIZING
	 */
	public boolean isClusterTopologyLocked() {
		// update the locally stored status from the distributed manager.
		ODistributedServerManager distributedManager = manager.getServer().getDistributedManager();
		for (String nodeName : distributedManager.getActiveServers()) {
			databaseStatusMap.put(nodeName, distributedManager.getDatabaseStatus(nodeName, "storage"));
		}
		for (Entry<String, DB_STATUS> entry : databaseStatusMap.entrySet()) {
			DB_STATUS status = entry.getValue();
			switch (status) {
			case BACKUP:
			case SYNCHRONIZING:
				if (log.isInfoEnabled()) {
					log.info("Current database stati: {}", getDatabaseStati());
					log.info("Locking since " + entry.getKey() + " is in status " + entry.getValue());
				}
				return true;
			default:
				continue;
			}
		}
		if (log.isDebugEnabled()) {
			log.debug("Current database stati: {}", getDatabaseStati());
		}
		return false;
	}

	/**
	 * Get a string representation of the current database stati
	 * @param fromOrientDB true to acquire the stati from OrientDB, false to get the stati from the map {@link #databaseStatusMap}
	 * @return string representation of database stati
	 */
	protected String getDatabaseStati() {
		Map<String, DB_STATUS> statusMap = new TreeMap<>();

		// capture the current status entries in a treemap (so that it is safe to iterate over the map, and the keys=nodes will be sorted in natural order)
		statusMap.putAll(databaseStatusMap);
		return statusMap.entrySet().stream().map(entry -> String.format("%s: %s", entry.getKey(), entry.getValue()))
				.collect(Collectors.joining(", "));
	}
}
