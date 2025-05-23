package com.gentics.mesh.database.cluster;

import static com.gentics.mesh.MeshEnv.CONFIG_FOLDERNAME;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.core.rest.admin.cluster.ClusterInstanceInfo;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.distributed.coordinator.MasterElector;
import com.gentics.mesh.distributed.coordinator.MeshMemberInfo;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.util.UUIDUtil;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import dagger.Lazy;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

/**
 * Hibernate DB cluster manager.
 * 
 * @author plyhun
 *
 */
@Singleton
public class HibClusterManager implements ClusterManager {
	private static final Logger log = LoggerFactory.getLogger(HibClusterManager.class);

	private final HibernateMeshOptions options;
	private final Lazy<Database> db;
	private HazelcastClusterManager clusterManager;
	private Lazy<MasterElector> masterElector;
	private HazelcastInstance hazelcastInstance;

	@Override
	public HazelcastInstance getHazelcast() {
		if (hazelcastInstance == null) {
			Config config = null;
			String hazelcastFilePath = new File(CONFIG_FOLDERNAME, "hazelcast.xml").getAbsolutePath();
			try {
				config = new XmlConfigBuilder(hazelcastFilePath).build();
			} catch (FileNotFoundException e) {
				config = new Config();
			}
			config.setInstanceName(options.getNodeName());
			config.setClusterName(options.getClusterOptions().getClusterName());
			config.setClassLoader(Thread.currentThread().getContextClassLoader());
			config.getMemberAttributeConfig().setAttribute("__vertx.nodeId", options.getNodeName());
			hazelcastInstance = Hazelcast.getOrCreateHazelcastInstance(config);
		}
		return hazelcastInstance;
	}

	@Override
	public io.vertx.core.spi.cluster.ClusterManager getVertxClusterManager() {
		if (clusterManager == null) {
			clusterManager = new HazelcastClusterManager(getHazelcast());
		}
		return clusterManager;
	}

	@Inject
	public HibClusterManager(HibernateMeshOptions options, Lazy<Database> db, Lazy<MasterElector> masterElector) {
		this.options = options;
		this.db = db;
		this.masterElector = masterElector;
	}

	@Override
	public void initConfigurationFiles() throws IOException {

	}

	@Override
	public ClusterStatusResponse getClusterStatus() {
		if (options.getClusterOptions().isEnabled()) {
			List<ClusterInstanceInfo> instances = getHazelcast().getCluster().getMembers().stream().map(member -> {
				ClusterInstanceInfo info = new ClusterInstanceInfo();
				info.setAddress(member.getAddress().toString());
				info.setUuid(UUIDUtil.toShortUuid(member.getUuid()));
				MeshMemberInfo memberInfo = masterElector.get().getMemberInfo().get(member.getUuid());
				if (memberInfo != null) {
					info.setRole(memberInfo.isMaster() ? "MASTER" : "");
					info.setStartDate(memberInfo.getStartedAt().toString());
					info.setName(memberInfo.getName());
				} else {
					log.error("No member info available for " + member.getUuid());
				}
				return info;
			}).collect(Collectors.toList());
			return new ClusterStatusResponse().setInstances(instances);
		} else {
			return new ClusterStatusResponse();
		}
	}
}
