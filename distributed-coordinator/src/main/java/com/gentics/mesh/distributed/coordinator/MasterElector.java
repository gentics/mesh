package com.gentics.mesh.distributed.coordinator;

import javax.inject.Inject;
import javax.inject.Singleton;

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

	private Lazy<HazelcastInstance> hazelcast;
	private ILock masterLock;

	/**
	 * Flag that is set, when the instance is merging (back) into the cluster
	 */
	private static boolean merging = false;

	// https://git.gentics.com/psc/contentnode/blob/dev/contentnode-lib%2Fsrc%2Fmain%2Fjava%2Fcom%2Fgentics%2Fcontentnode%2Fcluster%2FClusterSupport.java#L391

	@Inject
	public MasterElector(Lazy<HazelcastInstance> hazelcast) {
		this.hazelcast = hazelcast;
	}

	public void start() {
		masterLock = hazelcast.get().getLock(MASTER);

		electMaster();
		addMessageListeners();
	}

	public void stop() {

	}

	private void electMaster() {
		Cluster cluster = hazelcast.get().getCluster();

		log.info("Locking for master election");
		masterLock.lock();
		try {
			log.info("Locked for master election");
			boolean hasMaster = cluster.getMembers().stream()
				.filter(m -> isMaster(m))
				.findFirst().isPresent();
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
		// addr = member.getAddress();
		String name = member.getStringAttribute("name");
		System.out.println(name);
		return true;
	}

	/**
	 * Add message listeners
	 */
	private void addMessageListeners() {
		// Add membership listener for selecting a new master, if a node leaves the cluster
		hazelcast.get().getCluster().addMembershipListener(new MembershipListener() {

			@Override
			public void memberRemoved(MembershipEvent membershipEvent) {
				log.info(String.format("Removed %s", membershipEvent.getMember().getUuid()));
				if (isMaster(membershipEvent.getMember())) {
					electMaster();
				}
			}

			@Override
			public void memberAttributeChanged(MemberAttributeEvent memberAttributeEvent) {
			}

			@Override
			public void memberAdded(MembershipEvent membershipEvent) {
				log.info(String.format("Added %s", membershipEvent.getMember().getUuid()));
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

		// ITopic<Void> requestMasterTopic = hazelcast.getTopic(REQUEST_MASTER_TOPIC);
		// requestMasterTopic.addMessageListener(REQUEST_MASTER_LISTENER);

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

}
