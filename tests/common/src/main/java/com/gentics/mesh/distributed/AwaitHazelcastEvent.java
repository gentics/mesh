package com.gentics.mesh.distributed;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.topic.ITopic;

/**
 * {@link AutoCloseable} implementation which will wait for a message of a specific topic in {@link #close()}.
 */
public class AwaitHazelcastEvent implements AutoCloseable {
	/**
	 * Name of the hazelcast instance which is expected to get the message
	 */
	private String instanceName;

	/**
	 * Name of the topic of the expected message
	 */
	private String topicName;

	/**
	 * Timeout for waiting for the message
	 */
	private long timeout;

	/**
	 * Unit of the waiting timeout
	 */
	private TimeUnit unit;

	/**
	 * Hazelcast instance
	 */
	private HazelcastInstance hzInstance;

	/**
	 * UUID of the message listener
	 */
	private UUID listenerUuid;

	/**
	 * Countdown latch
	 */
	private CountDownLatch latch;

	/**
	 * Create an instance which will wait for a message in the hazelcast instance with given name.
	 * Creation will fail, if the hazelcast does not exist.
	 * @param instanceName name of the hazelcast instance
	 * @param topicName name of the message topic
	 * @param timeout waiting timeout
	 * @param unit unit of the waiting timeout
	 */
	public AwaitHazelcastEvent(String instanceName, String topicName, long timeout, TimeUnit unit) {
		this.instanceName = instanceName;
		this.topicName = topicName;
		this.timeout = timeout;
		this.unit = unit;
		hzInstance = Hazelcast.getHazelcastInstanceByName(instanceName);
		assertThat(hzInstance).as("Hazelcast instance " + instanceName).isNotNull();

		ITopic<?> topic = hzInstance.getTopic(topicName);
		latch = new CountDownLatch(1);
		listenerUuid = topic.addMessageListener(msg -> {
			latch.countDown();
		});
	}

	@Override
	public void close() throws Exception {
		try {
			if (latch != null) {
				if (!latch.await(timeout, unit)) {
					throw new Exception(
							"Timeout while waiting for event " + topicName + " in hazelcast instance " + instanceName);
				}
			}
		} finally {
			if (hzInstance != null && listenerUuid != null) {
				hzInstance.getTopic(topicName).removeMessageListener(listenerUuid);
			}
		}
	}
}
