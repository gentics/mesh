package com.gentics.mesh.core.verticle.migration.impl;

import static com.gentics.mesh.Events.MESH_MIGRATION;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.release.ReleaseVersionEdge;
import com.gentics.mesh.core.rest.admin.migration.MigrationInfo;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatusResponse;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.gentics.mesh.core.verticle.migration.MigrationStatusHandler;
import com.gentics.mesh.util.DateUtils;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.LocalMap;

/**
 * The migration status class keeps track of the status of a migration and manages also the errors and event handling.
 */
public class MigrationStatusHandlerImpl implements MigrationStatusHandler {

	private static final Logger log = LoggerFactory.getLogger(MigrationStatusHandlerImpl.class);

	private MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

	private Vertx vertx;

	private MigrationInfo info;

	private ReleaseVersionEdge versionEdge;

	private Job job;

	public MigrationStatusHandlerImpl(Job job, Vertx vertx, MigrationType type) {
		this.vertx = vertx;
		String startDate = DateUtils.toISO8601(System.currentTimeMillis());
		String nodeName = Mesh.mesh().getOptions().getNodeName();
		this.info = new MigrationInfo(job.getUuid(), type, startDate, nodeName);
		this.job = job;
	}

	@Override
	public MigrationStatusHandler updateStatus() {
		if (Mesh.mesh().getOptions().getClusterOptions().isEnabled()) {
			CountDownLatch latch = new CountDownLatch(1);
			// Get the cluster data map
			vertx.sharedData().getClusterWideMap(MIGRATION_DATA_MAP_KEY, rh -> {
				if (rh.failed()) {
					log.error("Could not load data map", rh.cause());
					latch.countDown();
				} else {
					// Get the json object from the map
					AsyncMap<Object, Object> map = rh.result();
					map.get("data", rd -> {
						if (rd.succeeded()) {
							MigrationStatusResponse response = (MigrationStatusResponse) rd.result();
							if (response == null) {
								response = new MigrationStatusResponse();
							}

							updateResponse(response);
							map.put("data", response, ph -> {
								if (ph.failed()) {
									log.error("Could not store updated entry in map.", ph.cause());
								}
								latch.countDown();
							});
						} else {
							log.error("Could not load data", rd.cause());
							latch.countDown();
						}
					});
				}
			});
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} else {
			LocalMap<Object, Object> map = Mesh.vertx().sharedData().getLocalMap(MIGRATION_DATA_MAP_KEY);
			MigrationStatusResponse response = (MigrationStatusResponse) map.get("data");
			// Check whether this might be the first time that the migration status is updated.
			if (response == null) {
				response = new MigrationStatusResponse();
			}

			updateResponse(response);

			if (map.containsKey("data")) {
				map.replace("data", response);
			} else {
				map.put("data", response);
			}
		}
		if (versionEdge != null) {
			versionEdge.setMigrationStatus(info.getStatus());
		}
		job.setCompletionCount(info.getCompleted());
		job.setStatus(info.getStatus());
		return this;

	}

	private void updateResponse(MigrationStatusResponse response) {
		// Locate the current entry.
		MigrationInfo currentInfo = response.getMigrations().stream().filter(e -> {
			return info.getUuid().equals(e.getUuid());
		}).findFirst().orElse(null);

		// We need to add the new migration to the list
		if (currentInfo != null) {
			response.getMigrations().remove(currentInfo);
		}

		// Update the info and add it to the list
		response.getMigrations().add(info);

		purgeOldEntries(response.getMigrations());
	}

	public void purgeOldEntries(List<MigrationInfo> list) {
		Set<MigrationInfo> oldInfos = new HashSet<>();
		list.stream().sorted().skip(MAX_MIGRATION_INFO_ENTRIES).map(e -> {
			if (log.isDebugEnabled()) {
				log.debug("Removed info with date {" + e.getStartDate() + "} from object.");
			}
			oldInfos.add(e);
			return null;
		}).count();
		// Finally remove the old infos
		list.removeAll(oldInfos);
	}

	// /**
	// * This is currently not working but will be supported by vert.x 3.5.x
	// *
	// * @param map
	// */
	// @Deprecated
	// private void purgeOldInfos(LocalMap<Object, Object> map) {
	// Set<Long> bogusKeys = new HashSet<>();
	// // Sort the entries by the initial timestamp, skip the amount of
	// // entries we want to keep and add the rest to the bogus keys list
	// map.entrySet().stream().sorted((o1, o2) -> {
	// Long key1 = (Long) o1.getKey();
	// Long key2 = (Long) o2.getKey();
	// return Long.compare(key1, key2);
	// }).skip(MAX_MIGRATION_DATE_ENTRIES).map(e -> {
	// bogusKeys.add((Long) e.getKey());
	// return null;
	// });
	// // Finally remove the bogus keys
	// for (Long key : bogusKeys) {
	// map.remove(key);
	// }
	//
	// }

	// /**
	// * This is currently not working but will be supported by vert.x 3.5.x
	// *
	// * @param map
	// */
	// @Deprecated
	// private void purgeOldInfos(AsyncMap<Object, Object> map) {
	// // MAX_MIGRATION_DATE_ENTRIES
	// // map.clear(resultHandler);
	//
	// }

	private ObjectName startJMX() throws MalformedObjectNameException {
		String JMX_MBEAN_NAME = "com.gentics.mesh:type=NodeMigration";
		ObjectName statusMBeanName = new ObjectName(JMX_MBEAN_NAME + ",name=" + "bogus");
		try {
			mbs.registerMBean(this, statusMBeanName);
		} catch (Exception e1) {
		}
		return statusMBeanName;
	}

	private MigrationStatusHandler stopJMX(ObjectName statusMBeanName) {
		try {
			mbs.unregisterMBean(statusMBeanName);
		} catch (Exception e1) {
		}
		return this;
	}

	/**
	 * This method will:
	 * <ul>
	 * <li>Log the success</li>
	 * <li>Reply to the invoker of the migration</li>
	 * <li>Send an event to other potential consumers on the eventbus</li>
	 * </ul>
	 */
	public MigrationStatusHandler done() {
		log.info("Migration completed without errors.");
		info.setStatus(MigrationStatus.COMPLETED);
		info.setStopDate(DateUtils.toISO8601(System.currentTimeMillis()));
		updateStatus();
		JsonObject result = new JsonObject().put("type", "completed");
		vertx.eventBus().publish(MESH_MIGRATION, result);
		if (versionEdge != null) {
			versionEdge.setMigrationStatus(info.getStatus());
		}
		job.setStopTimestamp(System.currentTimeMillis());
		return this;
	}

	/**
	 * This method will:
	 * <ul>
	 * <li>Log the error</li>
	 * <li>Reply to the invoker of the migration</li>
	 * <li>Send an event to other potential consumers on the eventbus</li>
	 * </ul>
	 * 
	 * @param error
	 * @param failureMessage
	 */
	public MigrationStatusHandler error(Throwable error, String failureMessage) {
		log.error("Error handling migration", error);
		info.setStatus(MigrationStatus.FAILED);
		info.setStopDate(DateUtils.toISO8601(System.currentTimeMillis()));
		info.setError(failureMessage + "\n\n" + ExceptionUtils.getStackTrace(error));
		updateStatus();
		vertx.eventBus().publish(MESH_MIGRATION, new JsonObject().put("type", "failed"));
		if (versionEdge != null) {
			versionEdge.setMigrationStatus(info.getStatus());
		}
		job.setStopTimestamp(System.currentTimeMillis());
		return this;
	}

	@Override
	public MigrationInfo getInfo() {
		return info;
	}

	@Override
	public void setVersionEdge(ReleaseVersionEdge versionEdge) {
		this.versionEdge = versionEdge;
	}

}
