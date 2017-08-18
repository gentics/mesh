package com.gentics.mesh.core.verticle.migration;

import static com.gentics.mesh.Events.MESH_MIGRATION;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.gentics.mesh.Mesh;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.LocalMap;

public abstract class AbstractMigrationStatus implements MigrationStatus {

	public static final String ERROR_STATUS = "error";
	public static final String RUNNING_STATUS = "running";
	public static final String STARTING_STATUS = "starting";
	public static final String COMPLETED_STATUS = "completed";

	protected MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

	private static final Logger log = LoggerFactory.getLogger(AbstractMigrationStatus.class);

	protected Vertx vertx;

	protected String sourceName;

	protected String sourceUuid;

	protected String sourceVersion;

	protected String targetVersion;

	protected Message<Object> message;

	protected MigrationType type;

	protected Long startTime;

	protected String status;

	protected int totalElements;

	protected int doneElements = 0;

	public AbstractMigrationStatus(Message<Object> message, Vertx vertx, MigrationType type) {
		this.message = message;
		this.vertx = vertx;
		this.type = type;
		this.startTime = System.currentTimeMillis();
		this.status = STARTING_STATUS;
	}

	@Override
	public String getSourceName() {
		return sourceName;
	}

	@Override
	public MigrationStatus setSourceName(String name) {
		this.sourceName = name;
		return this;
	}

	@Override
	public String getSourceUuid() {
		return sourceUuid;
	}

	@Override
	public MigrationStatus setSourceUuid(String sourceUuid) {
		this.sourceUuid = sourceUuid;
		return this;
	}

	@Override
	public String getSourceVersion() {
		return sourceVersion;
	}

	@Override
	public MigrationStatus setSourceVersion(String sourceVersion) {
		this.sourceVersion = sourceVersion;
		return this;
	}

	@Override
	public String getTargetVersion() {
		return targetVersion;
	}

	@Override
	public MigrationStatus setTargetVersion(String targetVersion) {
		this.targetVersion = targetVersion;
		return this;
	}

	@Override
	public Long getStartTime() {
		return startTime;
	}

	@Override
	public String getStatus() {
		return status;
	}

	@Override
	public int getTotalElements() {
		return totalElements;
	}

	@Override
	public void setTotalElements(int totalElements) {
		this.totalElements = totalElements;
	}

	@Override
	public int getDoneElements() {
		return doneElements;
	}

	@Override
	public void incDoneElements() {
		doneElements++;
	}

	@Override
	public MigrationType getType() {
		return type;
	}

	/**
	 * Create the json object which contains the migration status info.
	 * 
	 * @return
	 */
	public abstract JsonObject createInfoJson();

	@Override
	public void updateStatus(JsonObject info) {
		if (Mesh.mesh().getOptions().getClusterOptions().isEnabled()) {
			vertx.sharedData().getLock("mesh.data.lock", rhl -> {
				if (rhl.failed()) {
					log.warn("Could not update status since lock could not be acquired", rhl.cause());
				} else {
					// Get the cluster data map
					vertx.sharedData().getClusterWideMap(MIGRATION_DATA_MAP_KEY, rh -> {
						if (rh.failed()) {
							log.error("Could not load data map", rh.cause());
						} else {
							// Get the json object from the map
							AsyncMap<Object, Object> map = rh.result();
							map.get("data", rd -> {
								if (rd.succeeded()) {
									JsonObject obj = (JsonObject) rd.result();
									if (obj == null) {
										obj = new JsonObject();
									}
									String key = String.valueOf(getStartTime());
									obj.put(key, info);

									// TODO TESTCODE
									for (int i = 0; i < 30; i++) {
										obj.put(String.valueOf(i), info);
									}

									purgeOldEntries(obj);
									map.put("data", obj, ph -> {
										if (ph.failed()) {
											log.error("Could not store updated entry in map.", ph.cause());
										}
										rhl.result().release();
									});
								} else {
									log.error("Could not load data", rd.cause());
								}
							});
							// purgeOldInfos(map);
						}
					});
				}
			});
		} else {
			LocalMap<Object, Object> map = Mesh.vertx().sharedData().getLocalMap(MIGRATION_DATA_MAP_KEY);
			JsonObject obj = (JsonObject) map.get("data");
			if (obj == null) {
				obj = new JsonObject();
			}
			obj.put(String.valueOf(getStartTime()), createInfoJson());
			purgeOldEntries(obj);
			map.put("data", obj);
		}
	}

	public void purgeOldEntries(JsonObject obj) {
		Set<String> bogusKeys = new HashSet<>();
		obj.fieldNames().stream().sorted((o1, o2) -> {
			Long key1 = Long.valueOf(o1);
			Long key2 = Long.valueOf(o2);
			return Long.compare(key1, key2);
		}).skip(MAX_MIGRATION_DATE_ENTRIES).map(e -> {
			bogusKeys.add(e);
			return null;
		});
		// Finally remove the bogus keys
		for (String key : bogusKeys) {
			if (log.isDebugEnabled()) {
				log.debug("Removed {" + key + "} from object.");
			}
			obj.remove(key);
		}

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

	private void stopJMX(ObjectName statusMBeanName) {
		try {
			mbs.unregisterMBean(statusMBeanName);
		} catch (Exception e1) {
		}
	}

	/**
	 * This method will:
	 * <ul>
	 * <li>Log the success</li>
	 * <li>Reply to the invoker of the migration</li>
	 * <li>Send an event to other potential consumers on the eventbus</li>
	 * </ul>
	 * 
	 * @param message
	 */
	public void done(Message<Object> message) {
		log.info("Migration completed without errors.");
		this.status = COMPLETED_STATUS;
		updateStatus(createInfoJson());
		JsonObject result = new JsonObject().put("type", "completed");
		message.reply(result);
		vertx.eventBus().publish(MESH_MIGRATION, result);
	}

	/**
	 * This method will:
	 * <ul>
	 * <li>Log the error</li>
	 * <li>Reply to the invoker of the migration</li>
	 * <li>Send an event to other potential consumers on the eventbus</li>
	 * </ul>
	 * 
	 * @param message
	 * @param error
	 * @param failureMessage
	 */
	public void handleError(Message<Object> message, Throwable error, String failureMessage) {
		log.error("Error handling migration", error);
		this.status = ERROR_STATUS;
		updateStatus(createInfoJson());
		message.fail(100, failureMessage);
		vertx.eventBus().publish(MESH_MIGRATION, new JsonObject().put("type", "failed"));
	}

}
