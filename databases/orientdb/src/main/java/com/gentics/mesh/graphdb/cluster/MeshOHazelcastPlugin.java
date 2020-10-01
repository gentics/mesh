package com.gentics.mesh.graphdb.cluster;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.gentics.mesh.MeshEnv;
import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.util.OCallableNoParamNoReturn;
import com.orientechnologies.common.util.OCallableUtils;
import com.orientechnologies.orient.core.OSignalHandler;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.distributed.ODistributedException;
import com.orientechnologies.orient.server.hazelcast.OHazelcastDistributedMap;
import com.orientechnologies.orient.server.hazelcast.OHazelcastMergeStrategy;
import com.orientechnologies.orient.server.hazelcast.OHazelcastPlugin;
import com.orientechnologies.orient.server.network.OServerNetworkListener;

/**
 * Custom Hazelcast plugin for OrientDB which supports injecting a previously instantiated hazelcast instance.
 */
public class MeshOHazelcastPlugin extends OHazelcastPlugin {

	public static HazelcastInstance hazelcast;
	public static Config hazelcastConfig;

	@Override
	public HazelcastInstance configureHazelcast() throws FileNotFoundException {
		super.hazelcastInstance = hazelcast;
		super.hazelcastConfig = hazelcastConfig;
		return hazelcast;
	}

	@Override
	public HazelcastInstance getHazelcastInstance() {
		for (int retry = 1; hazelcast == null && !Thread.currentThread().isInterrupted(); ++retry) {
			if (retry > 25)
				throw new ODistributedException("Hazelcast instance is not available");

			// WAIT UNTIL THE INSTANCE IS READY, FOR MAXIMUM 5 SECS (25 x 200ms)
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				break;
			}
		}
		return hazelcast;
	}

	public static HazelcastInstance createHazelcast() {
		if (hazelcast == null) {
			File configFile = new File(MeshEnv.CONFIG_FOLDERNAME, "hazelcast.xml");
			if (!configFile.exists()) {
				throw new RuntimeException("Could not find configuration file for hazelcast.");
			}
			try {
				hazelcastConfig = new FileSystemXmlConfig(configFile.getAbsolutePath());
				hazelcastConfig.setClassLoader(MeshOHazelcastPlugin.class.getClassLoader());
				hazelcastConfig.getMapConfig(CONFIG_REGISTEREDNODES).setBackupCount(6);
				hazelcastConfig.getMapConfig(OHazelcastDistributedMap.ORIENTDB_MAP).setMergePolicy(OHazelcastMergeStrategy.class.getName());
				hazelcastConfig.setProperty("hazelcast.shutdownhook.enabled", "false");
				hazelcast = Hazelcast.newHazelcastInstance(hazelcastConfig);
			} catch (Throwable t) {
				throw new RuntimeException("Error while starting hazelcast", t);
			}
		}
		return hazelcast;
	}

	@Override
	public void shutdown() {
		if (!enabled) {
			return;
		}
		OSignalHandler signalHandler = Orient.instance().getSignalHandler();

		for (OServerNetworkListener nl : serverInstance.getNetworkListeners())
			nl.unregisterBeforeConnectNetworkEventListener(this);

		OLogManager.instance().warn(this, "Shutting down node '%s'...", nodeName);
		setNodeStatus(NODE_STATUS.SHUTTINGDOWN);

		try {
			final Set<String> databases = new HashSet<String>();

			if (hazelcastInstance.getLifecycleService().isRunning())
				for (Map.Entry<String, Object> entry : configurationMap.entrySet()) {
					if (entry.getKey().startsWith(CONFIG_DBSTATUS_PREFIX)) {

						final String nodeDb = entry.getKey().substring(CONFIG_DBSTATUS_PREFIX.length());

						if (nodeDb.startsWith(nodeName))
							databases.add(entry.getKey());
					}
				}

			// PUT DATABASES AS NOT_AVAILABLE
			for (String k : databases)
				configurationMap.put(k, DB_STATUS.NOT_AVAILABLE);

		} catch (HazelcastInstanceNotActiveException e) {
			// HZ IS ALREADY DOWN, IGNORE IT
		}

		if (membershipListenerRegistration != null) {
			try {
				hazelcastInstance.getCluster().removeMembershipListener(membershipListenerRegistration);
			} catch (HazelcastInstanceNotActiveException e) {
				// HZ IS ALREADY DOWN, IGNORE IT
			}
		}

		OCallableUtils.executeIgnoringAnyExceptions(new OCallableNoParamNoReturn() {
			@Override
			public void call() {
				configurationMap.destroy();
			}
		});

		OCallableUtils.executeIgnoringAnyExceptions(new OCallableNoParamNoReturn() {
			@Override
			public void call() {
				configurationMap.getHazelcastMap().removeEntryListener(membershipListenerMapRegistration);
			}
		});

		setNodeStatus(NODE_STATUS.OFFLINE);
		OServer.unregisterServerInstance(getLocalNodeName());
	}

}
