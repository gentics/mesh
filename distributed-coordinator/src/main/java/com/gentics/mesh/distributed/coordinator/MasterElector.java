package com.gentics.mesh.distributed.coordinator;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigResponse;
import com.gentics.mesh.core.rest.admin.cluster.ClusterServerConfig;
import com.gentics.mesh.core.rest.admin.cluster.ServerRole;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.hazelcast.core.Cluster;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberAttributeEvent;
import com.hazelcast.core.MembershipEvent;
import com.hazelcast.core.MembershipListener;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class MasterElector {

	private static final Logger log = LoggerFactory.getLogger(MasterElector.class);

	/**
	 * Key of the master lock and the master attribute
	 */
	private final static String MASTER = "master";

	private final static String MESH_HTTP_PORT_ATTR = "mesh_http_port";

	private final static String MESH_NODE_NAME_ATTR = "mesh_node_name";

	private final MeshOptions options;

	private final Database database;

	private final Lazy<HazelcastInstance> hazelcast;

	protected ILock masterLock;
	protected Member masterMember;
	protected String localUuid;
	protected Pattern coordinatorRegex;

	/**
	 * Flag that is set, when the instance is merging (back) into the cluster
	 */
	private static boolean merging = false;

	@Inject
	public MasterElector(Lazy<HazelcastInstance> hazelcast, MeshOptions options, Database database) {
		this.hazelcast = hazelcast;
		this.options = options;
		this.database = database;

		// Setup regex for master election
		String regexStr = options.getClusterOptions().getCoordinatorRegex();
		if (regexStr != null) {
			try {
				coordinatorRegex = Pattern.compile(regexStr);
			} catch (PatternSyntaxException e) {
				throw new RuntimeException("Could not compile coordinator regex from string {" + regexStr + "}", e);
			}
		}
	}

	public void start() {
		HazelcastInstance hz = hazelcast.get();
		masterLock = hz.getLock(MASTER);
		Member localMember = hz.getCluster().getLocalMember();
		localUuid = localMember.getUuid();
		int port = options.getHttpServerOptions().getPort();
		localMember.setIntAttribute(MESH_HTTP_PORT_ATTR, port);
		localMember.setStringAttribute(MESH_NODE_NAME_ATTR, options.getNodeName());
		addMessageListeners();
		electMaster();
		findCurrentMaster();
	}

	public void stop() {

	}

	/**
	 * Each instance in the cluster will call the elect master method when the structure of the cluster changes. The master election runs in a locked manner and
	 * is terminated as soon as one node in the cluster got elected.
	 * 
	 * @return Elected member
	 */
	public void electMaster() {
		Cluster cluster = hazelcast.get().getCluster();

		log.info("Locking for master election");
		masterLock.lock();
		try {
			log.info("Locked for master election");
			Optional<Member> foundMaster = cluster.getMembers().stream()
				.filter(m -> isMaster(m))
				.findFirst();
			boolean hasMaster = foundMaster.isPresent();
			boolean isElectible = isElectible(cluster.getLocalMember());
			if (!hasMaster && isElectible) {
				cluster.getLocalMember().setBooleanAttribute(MASTER, true);
				log.info("Cluster node was elected as new master");
			} else if (cluster.getMembers().stream()
				.filter(m -> isMaster(m))
				.count() > 1) {
				log.info("Detected multiple masters in the cluster, giving up the master flag");
				giveUpMasterFlag();
			}
		} finally {
			masterLock.unlock();
			log.info("Unlocked after master election");
		}
	}

	/**
	 * Check whether the member is allowed to be elected as master
	 * 
	 * @param m
	 * @return
	 */
	private boolean isElectible(Member member) {
		String name = member.getStringAttribute(MESH_NODE_NAME_ATTR);

		// Check whether name of the node matches the coordinator regex.
		if (coordinatorRegex != null) {
			Matcher m = coordinatorRegex.matcher(name);
			if (!m.find()) {
				log.info("Node {" + name + "} was not accepted by provided regex.");
				return false;
			}
		}

		ClusterConfigResponse config = database.loadClusterConfig();
		Optional<ClusterServerConfig> databaseServer = config.getServers().stream().filter(s -> s.getName().equals(name)).findFirst();
		if (databaseServer.isPresent()) {
			// Replicas are not eligible for master election
			ServerRole role = databaseServer.get().getRole();
			if (role == ServerRole.REPLICA) {
				log.info("Node {" + name + "} is a replica and thus not eligable for election.");
				return false;
			}
		}
		// TODO test connection?
		return true;
	}

	/**
	 * Add message listeners
	 */
	private void addMessageListeners() {
		// Add membership listener for selecting a new master, if a node leaves the cluster
		Cluster cluster = hazelcast.get().getCluster();
		cluster.addMembershipListener(new MembershipListener() {

			@Override
			public void memberRemoved(MembershipEvent membershipEvent) {
				log.info(String.format("Removed %s", membershipEvent.getMember().getUuid()));
				if (isMaster(membershipEvent.getMember())) {
					electMaster();
				}
				findCurrentMaster();
			}

			@Override
			public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
				if (memberAttributeEvent.getKey().equals(MASTER)) {
					findCurrentMaster();
				}
			}

			@Override
			public void memberAdded(MembershipEvent membershipEvent) {
				log.info(String.format("Added %s", membershipEvent.getMember().getUuid()));
				findCurrentMaster();
			}
		});

		hazelcast.get().getLifecycleService().addLifecycleListener(event -> {
			log.info(String.format("Lifecycle state changed to %s", event.getState()));
			switch (event.getState()) {
			case MERGING:
				merging = true;
				break;
			case MERGED:
				// when the instance merged into a cluster, we need to elect a new master (to avoid multimaster situations)
				merging = false;
				electMaster();
				break;
			default:
				break;
			}
		});
	}

	protected void findCurrentMaster() {
		Cluster cluster = hazelcast.get().getCluster();
		Optional<Member> master = cluster.getMembers().stream()
			.filter(m -> isMaster(m))
			.findFirst();
		if (master.isPresent()) {
			masterMember = master.get();
			log.info("Updated master member {" + masterMember.getStringAttribute(MESH_NODE_NAME_ATTR) + "}");
		}
	}

	/**
	 * Give up the master flag
	 */
	private void giveUpMasterFlag() {
		Member localMember = hazelcast.get().getCluster().getLocalMember();
		if (isMaster(localMember)) {
			localMember.setBooleanAttribute(MASTER, false);
		}
	}

	public boolean isMaster() {
		if (merging) {
			return false;
		}
		return isMaster(hazelcast.get().getCluster().getLocalMember());
	}

	/**
	 * Check whether the given member currently is the master
	 * 
	 * @param member
	 *            member
	 * @return true for the master
	 */
	private static boolean isMaster(Member member) {
		return member.getBooleanAttribute(MASTER) == Boolean.TRUE;
	}

	public boolean isLocal(Member member) {
		return localUuid.equals(member.getUuid());
	}

	public MasterServer getMasterMember() {
		if (masterMember == null) {
			return null;
		}
		String name = masterMember.getStringAttribute(MESH_NODE_NAME_ATTR);
		int port = masterMember.getIntAttribute(MESH_HTTP_PORT_ATTR);
		boolean isMasterLocal = isLocal(masterMember);
		String host = "localhost";
		if (!isMasterLocal) {
			host = masterMember.getAddress().getHost();
		}
		MasterServer server = new MasterServer(name, host, port, isMasterLocal);
		if (log.isDebugEnabled()) {
			log.debug("Our master member:" + server);
		}
		return server;
	}

}
