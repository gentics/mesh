package com.gentics.mesh.graphdb.cluster;

import java.io.FileNotFoundException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceNotActiveException;
import com.orientechnologies.common.io.OFileUtils;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.parser.OSystemVariableResolver;
import com.orientechnologies.common.util.OCallableNoParamNoReturn;
import com.orientechnologies.common.util.OCallableUtils;
import com.orientechnologies.orient.core.OSignalHandler;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.db.ODefaultEmbeddedDatabaseInstanceFactory;
import com.orientechnologies.orient.server.OServer;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.DB_STATUS;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager.NODE_STATUS;
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

	public static HazelcastInstance createHazelcast(OServerParameterConfiguration[] iParams) throws FileNotFoundException {
		if (hazelcast == null) {
			String hazelcastConfigFile = null;
			for (OServerParameterConfiguration param : iParams) {
				if (param.name.equalsIgnoreCase("configuration.hazelcast")) {
					hazelcastConfigFile = OSystemVariableResolver.resolveSystemVariables(param.value);
					hazelcastConfigFile = OFileUtils.getPath(hazelcastConfigFile);
				}
			}

			if (hazelcastConfigFile == null) {
				throw new RuntimeException("Could not determine configuration setting.");
			}

			hazelcastConfig = new FileSystemXmlConfig(hazelcastConfigFile);
			hazelcastConfig.setClassLoader(MeshOHazelcastPlugin.class.getClassLoader());
			hazelcastConfig.getMapConfig(CONFIG_REGISTEREDNODES).setBackupCount(6);
			hazelcastConfig.getMapConfig(OHazelcastDistributedMap.ORIENTDB_MAP).setMergePolicy(OHazelcastMergeStrategy.class.getName());
			hazelcast = Hazelcast.newHazelcastInstance(hazelcastConfig);
		}
		return hazelcast;
	}

	@Override
	public void shutdown() {
		if (!enabled)
			return;
		OSignalHandler signalHandler = Orient.instance().getSignalHandler();
		//if (signalHandler != null)
			//signalHandler.unregisterListener(signalListener);

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

//		try {
//			//super.shutdown();
//		} catch (HazelcastInstanceNotActiveException e) {
//			// HZ IS ALREADY DOWN, IGNORE IT
//		}

		if (membershipListenerRegistration != null) {
			try {
				hazelcastInstance.getCluster().removeMembershipListener(membershipListenerRegistration);
			} catch (HazelcastInstanceNotActiveException e) {
				// HZ IS ALREADY DOWN, IGNORE IT
			}
		}

		if (hazelcastInstance != null)
//			try {
//				hazelcastInstance.shutdown();
//			} catch (Exception e) {
//				OLogManager.instance().error(this, "Error on shutting down Hazelcast instance", e);
//			} finally {
//				hazelcastInstance = null;
//			}

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

		serverInstance.getDatabases().replaceFactory(new ODefaultEmbeddedDatabaseInstanceFactory());
		setNodeStatus(NODE_STATUS.OFFLINE);
		OServer.unregisterServerInstance(getLocalNodeName());
	}

}
