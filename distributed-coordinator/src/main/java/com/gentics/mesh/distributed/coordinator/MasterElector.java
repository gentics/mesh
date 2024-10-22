package com.gentics.mesh.distributed.coordinator;

import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.admin.cluster.ClusterConfigResponse;
import com.gentics.mesh.core.rest.admin.cluster.ClusterServerConfig;
import com.gentics.mesh.core.rest.admin.cluster.ServerRole;
import com.gentics.mesh.etc.config.ClusterOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.cluster.CoordinationTopology;
import com.hazelcast.cluster.Cluster;
import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;

import dagger.Lazy;

/**
 * Class which manages the election of the coordination master instance.
 */
@Singleton
public class MasterElector {

	private static final Logger log = LoggerFactory.getLogger(MasterElector.class);

	/**
	 * Key of the master lock and the master attribute
	 */
	private final static String MASTER = "master";

	/**
	 * Members map key
	 */
	private final static String MEMBERS = "members";

	/**
	 * Name of the Topic for requesting the master status
	 */
	private final static String REQUEST_MASTER_TOPIC = "request.master";

	private final MeshOptions options;
	private final ClusterOptions clusterOptions;

	private final Database database;

	private final Lazy<HazelcastInstance> hazelcast;

	protected IMap<Object, Object> masterLock;
	protected IMap<UUID, MeshMemberInfo> members;
	protected Member masterMember;
	protected UUID localUuid;
	protected Pattern coordinatorRegex;

	/**
	 * Flag that is set, when the instance is merging (back) into the cluster
	 */
	private boolean merging = false;

	@Inject
	public MasterElector(Lazy<HazelcastInstance> hazelcast, MeshOptions options, Database database) {
		this.hazelcast = hazelcast;
		this.options = options;
		this.clusterOptions = options.getClusterOptions();
		this.database = database;

		// Setup regex for master election
		String regexStr = clusterOptions.getCoordinatorRegex();
		if (regexStr != null) {
			try {
				coordinatorRegex = Pattern.compile(regexStr);
			} catch (PatternSyntaxException e) {
				throw new RuntimeException("Could not compile coordinator regex from string {" + regexStr + "}", e);
			}
		}
	}

	/**
	 * Start the elector which will prepare the hazelcast settings and elect or find the master.
	 */
	public void start() {
		HazelcastInstance hz = hazelcast.get();
		masterLock = hz.getMap(MASTER);
		members = hz.getMap(MEMBERS);
		Member localMember = localMember();
		localUuid = localMember.getUuid();
		int port = options.getHttpServerOptions().getPort();
		MeshMemberInfo info = new MeshMemberInfo(options.getNodeName(), port, Instant.now());
		members.put(localMember.getUuid(), info);
		addMessageListeners();
		electMaster();
		findCurrentMaster();
	}

	/**
	 * Check whether the instance that runs this code is the elected master.
	 *
	 * @return
	 */
	public boolean isMaster() {
		if (merging) {
			return false;
		}
		return isMaster(localMember());
	}

	/**
	 * Make this instance the master (if it not already is the master)
	 */
	public void setMaster() {
		if (isMaster()) {
			return;
		}

		HazelcastInstance instance = hazelcast.get();
		if (instance != null) {
			ITopic<Void> requestMasterTopic = instance.getTopic(REQUEST_MASTER_TOPIC);
			requestMasterTopic.publish(null);
		}
	}

	/**
	 * Each instance in the cluster will call the elect master method when the structure of the cluster changes. The master election runs in a locked manner and
	 * is terminated as soon as one node in the cluster got elected.
	 *
	 * @return Elected member
	 */
	private void electMaster() {
		Cluster cluster = hazelcast.get().getCluster();

		masterLock.lock(MASTER);
		try {
			log.info("Locked for master election");
			Optional<Member> foundMaster = cluster.getMembers().stream()
					.filter(m -> isMaster(m))
					.findFirst();
			boolean hasMaster = foundMaster.isPresent();
			boolean isElectible = isElectable(localMember());
			if (!hasMaster && isElectible) {
				if (clusterOptions.getCoordinatorTopology() == CoordinationTopology.MASTER_REPLICA) {
					database.setToMaster();
				}
				MeshMemberInfo info = members.get(localMember().getUuid());
				info.setMaster(true);
				members.put(localMember().getUuid(), info);
				log.info("Cluster node was elected as new master");
			} else if (cluster.getMembers().stream()
					.filter(m -> isMaster(m))
					.count() > 1) {
				log.info("Detected multiple masters in the cluster, giving up the master flag");
				giveUpMasterFlag();
			}
		} finally {
			masterLock.unlock(MASTER);
			log.info("Unlocked after master election");
		}
	}

	/**
	 * Check whether the member is allowed to be elected as master
	 *
	 * @param m
	 * @return
	 */
	private boolean isElectable(Member member) {
		String name = members.get(member.getUuid()).getName();

		// Check whether name of the node matches the coordinator regex.
		if (coordinatorRegex != null) {
			Matcher m = coordinatorRegex.matcher(name);
			if (!m.matches()) {
				log.info("Node {" + name + "} was not accepted by provided regex.");
				return false;
			}
		}

		if (clusterOptions.getCoordinatorTopology() == CoordinationTopology.UNMANAGED) {
			ClusterConfigResponse config = database.loadClusterConfig();
			Optional<ClusterServerConfig> databaseServer = config.getServers().stream().filter(s -> s.getName().equals(name)).findFirst();
			if (databaseServer.isPresent()) {
				// Replicas are not eligible for master election
				ServerRole role = databaseServer.get().getRole();
				if (role == ServerRole.REPLICA) {
					log.info("Node {" + name + "} is a replica and thus not eligible for election.");
					return false;
				}
			}
		}

		// TODO test connection?
		return true;
	}

	/**
	 * Get current cluster members info.
	 * 
	 * @return
	 */
	public Map<UUID, MeshMemberInfo> getMemberInfo() {
		return Collections.unmodifiableMap(members.getAll(members.keySet()));
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
				log.info("Removed member {}", membershipEvent.getMember().getUuid());
				if (isMaster(membershipEvent.getMember())) {
					electMaster();
				}
				findCurrentMaster();
			}

			@Override
			public void memberAdded(MembershipEvent membershipEvent) {
				log.info("Added member: {}", membershipEvent.getMember().getUuid());
				findCurrentMaster();
			}
		});

		hazelcast.get().getLifecycleService().addLifecycleListener(event -> {
			log.info("Lifecycle state changed to {}", event.getState());
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

		/**
		 * Request master message listener
		 */
		MessageListener<Void> REQUEST_MASTER_LISTENER = msg -> {
			executeIfNotFromLocal(msg, m -> {
				giveUpMasterFlag();
			});
			executeIfFromLocal(msg, m -> {
				if (clusterOptions.getCoordinatorTopology() == CoordinationTopology.MASTER_REPLICA) {
					database.setToMaster();
				}
				Member localMember = localMember();
				MeshMemberInfo info = members.get(localMember.getUuid());
				info.setMaster(true);
				members.put(localMember.getUuid(), info);
			});
		};

		ITopic<Void> requestMasterTopic = hazelcast.get().getTopic(REQUEST_MASTER_TOPIC);
		requestMasterTopic.addMessageListener(REQUEST_MASTER_LISTENER);

	}

	protected void findCurrentMaster() {
		Cluster cluster = hazelcast.get().getCluster();
		Optional<Member> master = cluster.getMembers().stream()
				.filter(m -> isMaster(m))
				.findFirst();
		if (master.isPresent()) {
			masterMember = master.get();
			log.info("Updated master member {" + members.get(masterMember.getUuid()).getName() + "}");
		} else {
			log.warn("Could not find master member in cluster.");
			masterMember = null;
		}
	}

	/**
	 * Give up the master flag
	 */
	private void giveUpMasterFlag() {
		Member localMember = localMember();
		if (isMaster(localMember)) {
			MeshMemberInfo info = members.get(localMember.getUuid());
			info.setMaster(false);
			members.put(localMember.getUuid(), info);
		}
	}

	/**
	 * Check whether the given member currently is the master
	 *
	 * @param member
	 *            member
	 * @return true for the master
	 */
	private boolean isMaster(Member member) {
		IMap<UUID, MeshMemberInfo> members = hazelcast.get().getMap(MEMBERS);
		return members.containsKey(member.getUuid()) && members.get(member.getUuid()).isMaster();
	}

	/**
	 * Check whether the given instance is the local instance.
	 *
	 * @param member
	 * @return
	 */
	public boolean isLocal(Member member) {
		return localUuid.equals(member.getUuid());
	}

	/**
	 * Return the hazelcast member for this instance.
	 *
	 * @return
	 */
	public Member localMember() {
		return hazelcast.get().getCluster().getLocalMember();
	}

	/**
	 * Get the server, which is currently the master, may be null
	 * @return current master, may be null
	 */
	public MasterServer getMasterMember() {
		if (masterMember == null) {
			return null;
		}
		String name = members.get(masterMember.getUuid()).getName();
		int port = members.get(masterMember.getUuid()).getPort();
		boolean isMasterLocal = isLocal(masterMember);
		String host = "localhost";
		if (!isMasterLocal) {
			host = masterMember.getAddress().getHost();
		}
		MasterServer server = new MasterServer(name, host, port, isMasterLocal);
		if (log.isDebugEnabled()) {
			log.debug("Our master member: " + server);
		}
		return server;
	}

	/**
	 * Let the handler accept the object from the given message, if the message was not published from the local node
	 *
	 * @param msg
	 *            message
	 * @param handler
	 *            handler
	 */
	private <T> void executeIfNotFromLocal(Message<T> msg, Consumer<T> handler) {
		Member publishingMember = msg.getPublishingMember();
		if (publishingMember != null && !publishingMember.localMember()) {
			handler.accept(msg.getMessageObject());
		}
	}

	/**
	 * Let the handler accept the object from the given message, if the message was published from the local node
	 *
	 * @param msg
	 *            message
	 * @param handler
	 *            handler
	 */
	private <T> void executeIfFromLocal(Message<T> msg, Consumer<T> handler) {
		Member publishingMember = msg.getPublishingMember();
		if (publishingMember != null && publishingMember.localMember()) {
			handler.accept(msg.getMessageObject());
		}
	}

	/**
	 * Check whether the local member is electable.
	 *
	 * @return
	 */
	public boolean isElectable() {
		if (!clusterOptions.isEnabled()) {
			return false;
		}
		return isElectable(localMember());
	}
}
