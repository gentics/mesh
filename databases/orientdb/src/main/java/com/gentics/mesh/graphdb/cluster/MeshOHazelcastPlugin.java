package com.gentics.mesh.graphdb.cluster;

import java.io.FileNotFoundException;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.orientechnologies.common.io.OFileUtils;
import com.orientechnologies.common.parser.OSystemVariableResolver;
import com.orientechnologies.orient.server.config.OServerParameterConfiguration;
import com.orientechnologies.orient.server.hazelcast.OHazelcastDistributedMap;
import com.orientechnologies.orient.server.hazelcast.OHazelcastMergeStrategy;
import com.orientechnologies.orient.server.hazelcast.OHazelcastPlugin;

public class MeshOHazelcastPlugin extends OHazelcastPlugin {

	public static HazelcastInstance hazelcast;
	public static Config hazelcastConfig;

	@Override
	public HazelcastInstance configureHazelcast() throws FileNotFoundException {
		super.hazelcastInstance = hazelcast;
		super.hazelcastConfig = hazelcastConfig;
		return hazelcast;
	}

	// If hazelcastConfig is null, use the file system XML config.
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
//			// Disabled the shudown hook of hazelcast, shutdown is managed by orient hook
//			hazelcastConfig.setProperty("hazelcast.shutdownhook.enabled", "false");

			hazelcast = Hazelcast.newHazelcastInstance(hazelcastConfig);
		}
		return hazelcast;
	}

}
