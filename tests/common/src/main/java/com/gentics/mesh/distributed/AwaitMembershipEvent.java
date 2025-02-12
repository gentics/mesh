package com.gentics.mesh.distributed;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.hazelcast.cluster.Cluster;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;

/**
 * {@link AutoCloseable} implementation which will wait for a membership event in {@link #close()}.
 */
public class AwaitMembershipEvent implements AutoCloseable {
	/**
	 * Timeout for waiting for the event
	 */
	private long timeout;

	/**
	 * Unit of the waiting timeout
	 */
	private TimeUnit unit;

	/**
	 * UUID of the listener
	 */
	private UUID listenerUuid;

	/**
	 * Countdown latch
	 */
	private CountDownLatch latch;

	/**
	 * Cluster, which is expected to get the membership event
	 */
	private Cluster hzCluster;

	/**
	 * Create an instance which will wait for a membership event in the given cluster in {@link #close()}.
	 * @param hzCluster hazelcast cluster
	 * @param timeout waiting timeout
	 * @param unit waiting timeout unit
	 */
	public AwaitMembershipEvent(Cluster hzCluster, long timeout, TimeUnit unit) {
		this.hzCluster = hzCluster;
		this.timeout = timeout;
		this.unit = unit;

		latch = new CountDownLatch(1);
		listenerUuid = hzCluster.addMembershipListener(new MembershipListener() {
			@Override
			public void memberRemoved(MembershipEvent membershipEvent) {
				latch.countDown();
			}
			
			@Override
			public void memberAdded(MembershipEvent membershipEvent) {
				latch.countDown();
			}
		});
	}

	@Override
	public void close() throws Exception {
		try {
			if (latch != null) {
				if (!latch.await(timeout, unit)) {
					throw new Exception(
							"Timeout while waiting for membership event in the cluster");
				}
			}
		} finally {
			if (listenerUuid != null) {
				hzCluster.removeMembershipListener(listenerUuid);
			}
		}
	}
}
