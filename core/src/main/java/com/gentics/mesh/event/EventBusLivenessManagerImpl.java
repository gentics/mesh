package com.gentics.mesh.event;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.db.cluster.ClusterManager;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.admin.cluster.ClusterInstanceInfo;
import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.monitor.liveness.EventBusLivenessManager;
import com.gentics.mesh.monitor.liveness.LivenessManager;

import dagger.Lazy;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Implementation of the {@link EventBusLivenessManager}, which will regularly publish events, which are consumed both
 * locally and in the cluster to check, whether the eventBus works.
 */
@Singleton
public final class EventBusLivenessManagerImpl implements EventBusLivenessManager {
	/**
	 * Name of the thread
	 */
	private static final String MESH_EVENTBUS_CHECKER_THREAD_NAME = "mesh-eventbus-checker";

	/**
	 * Logger
	 */
	private static Logger log = LoggerFactory.getLogger(EventBusLivenessManagerImpl.class);

	/**
	 * Timestamp of the last received ping
	 */
	private long lastPingTimestamp = -1;

	/**
	 * Map of timestamps for last received pings from cluster members
	 */
	private Map<String, Long> lastClusterPingTimestamps = Collections.synchronizedMap(new HashMap<>());

	/**
	 * Vert.x
	 */
	private final Lazy<Vertx> vertx;

	/**
	 * Liveness Manager
	 */
	private final LivenessManager livenessManager;

	/**
	 * Cluster Manager
	 */
	private final ClusterManager clusterManager;

	/**
	 * options
	 */
	private final MeshOptions options;

	/**
	 * Executor service for running the eventBus check
	 */
	private ScheduledExecutorService eventBusCheckerService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			return new Thread(r, MESH_EVENTBUS_CHECKER_THREAD_NAME);
		}
	});

	/**
	 * Create the instance
	 * @param vertx vertx
	 * @param livenessManager liveness manager
	 * @param clusterManager cluster manager
	 * @param options options
	 */
	@Inject
	public EventBusLivenessManagerImpl(Lazy<Vertx> vertx, LivenessManager livenessManager, ClusterManager clusterManager, MeshOptions options) {
		this.vertx = vertx;
		this.livenessManager = livenessManager;
		this.clusterManager = clusterManager;
		this.options = options;
	}

	@Override
	public void startRegularChecks() {
		int checkInterval = options.getVertxOptions().getEventBusOptions().getCheckInterval();
		if (checkInterval <= 0) {
			return;
		}

		EventBus eb = vertx.get().eventBus();
		eb.localConsumer(MeshEvent.PING_LOCAL.address, message -> {
			log.debug("Handling local ping");
			lastPingTimestamp = System.currentTimeMillis();
		});

		if (options.getClusterOptions().isEnabled()) {
			eb.consumer(MeshEvent.PING_CLUSTER.address, message -> {
				String sender = message.headers().get(EventQueueBatch.SENDER_HEADER);
				if (sender != null) {
					log.debug("Handling cluster ping from {}", sender);
					lastClusterPingTimestamps.put(sender, System.currentTimeMillis());
				}
			});
		}

		eventBusCheckerService.scheduleAtFixedRate(() -> {
			log.debug("Sending local ping");
			eb.publish(MeshEvent.PING_LOCAL.address, null);

			int errorThreshold = options.getVertxOptions().getEventBusOptions().getErrorThreshold();
			int warnThreshold = options.getVertxOptions().getEventBusOptions().getWarnThreshold();

			// when we already received a local ping, check when this happened
			if (lastPingTimestamp > 0) {
				long sinceLastPing = System.currentTimeMillis() - lastPingTimestamp;
				if (errorThreshold > 0 && sinceLastPing > errorThreshold) {
					log.error("Last local ping received {} ms ago", sinceLastPing);
					livenessManager.setLive(false, "Last local ping received " + sinceLastPing + " ms ago");
				} else if (warnThreshold > 0 && sinceLastPing > warnThreshold) {
					log.warn("Last local ping received {} ms ago", sinceLastPing);
				} else {
					log.debug("Last local ping received {} ms ago", sinceLastPing);
				}
			}

			if (options.getClusterOptions().isEnabled()) {
				log.debug("Sending cluster ping");
				eb.publish(MeshEvent.PING_CLUSTER.address, null,
						new DeliveryOptions().addHeader(EventQueueBatch.SENDER_HEADER, options.getNodeName()));

				// get the currently known cluster node names
				ClusterStatusResponse clusterStatus = clusterManager.getClusterStatus();
				Set<String> nodeNames = clusterStatus.getInstances().stream().map(ClusterInstanceInfo::getName).collect(Collectors.toSet());

				// remove all unknown cluster nodes from the map of pings
				lastClusterPingTimestamps.keySet().retainAll(nodeNames);

				nodeNames.forEach(nodeName -> {
					// when we already received a cluster ping from the node, check when this happened
					long lastNodePingTimestamp = lastClusterPingTimestamps.getOrDefault(nodeName, -1L);
					if (lastNodePingTimestamp > 0) {
						long sinceLastPing = System.currentTimeMillis() - lastNodePingTimestamp;
						if (errorThreshold > 0 && sinceLastPing > errorThreshold) {
							log.error("Last ping from {} received {} ms ago", nodeName, sinceLastPing);
						} else if (warnThreshold > 0 && sinceLastPing > warnThreshold) {
							log.warn("Last ping from {} received {} ms ago", nodeName, sinceLastPing);
						} else {
							log.debug("Last ping from {} received {} ms ago", nodeName, sinceLastPing);
						}
					}
				});
			}
		}, 0, checkInterval, TimeUnit.MILLISECONDS);
	}
}
