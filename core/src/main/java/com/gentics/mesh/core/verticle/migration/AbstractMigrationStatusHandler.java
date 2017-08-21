package com.gentics.mesh.core.verticle.migration;

import static com.gentics.mesh.Events.MESH_MIGRATION;

import java.lang.management.ManagementFactory;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.admin.MigrationInfo;
import com.gentics.mesh.core.rest.admin.MigrationStatusResponse;
import com.gentics.mesh.core.rest.admin.MigrationType;
import com.gentics.mesh.util.DateUtils;
import com.jayway.jsonpath.internal.function.numeric.Min;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.shareddata.AsyncMap;
import io.vertx.core.shareddata.LocalMap;

/**
 * Abstract migration handler which contains the basic methods to implement a custom status handler.
 */
public abstract class AbstractMigrationStatusHandler implements MigrationStatusHandler {

	public static final String ERROR_STATUS = "error";
	public static final String RUNNING_STATUS = "running";
	public static final String STARTING_STATUS = "starting";
	public static final String COMPLETED_STATUS = "completed";

	protected MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

	private static final Logger log = LoggerFactory.getLogger(AbstractMigrationStatusHandler.class);

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

	public AbstractMigrationStatusHandler(Message<Object> message, Vertx vertx, MigrationType type) {
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
	public MigrationStatusHandler setSourceName(String name) {
		this.sourceName = name;
		return this;
	}

	@Override
	public String getSourceUuid() {
		return sourceUuid;
	}

	@Override
	public MigrationStatusHandler setSourceUuid(String sourceUuid) {
		this.sourceUuid = sourceUuid;
		return this;
	}

	@Override
	public String getSourceVersion() {
		return sourceVersion;
	}

	@Override
	public MigrationStatusHandler setSourceVersion(String sourceVersion) {
		this.sourceVersion = sourceVersion;
		return this;
	}

	@Override
	public String getTargetVersion() {
		return targetVersion;
	}

	@Override
	public MigrationStatusHandler setTargetVersion(String targetVersion) {
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
	public MigrationStatusHandler setTotalElements(int totalElements) {
		this.totalElements = totalElements;
		return this;
	}

	@Override
	public int getDoneElements() {
		return doneElements;
	}

	@Override
	public MigrationStatusHandler incDoneElements() {
		doneElements++;
		return this;
	}

	@Override
	public MigrationType getType() {
		return type;
	}

	/**
	 * Create the info object which contains the migration status.
	 * 
	 * @return
	 */
	public abstract MigrationInfo createInfo();

	@Override
	public MigrationStatusHandler updateStatus(MigrationInfo info) {
		String startDate = DateUtils.toISO8601(getStartTime());

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
									MigrationStatusResponse response = (MigrationStatusResponse) rd.result();
									if (response == null) {
										response = new MigrationStatusResponse();
									}

									MigrationInfo currentInfo = response.getMigrations().stream().filter(e -> startDate.equals(e.getStartDate()))
											.findFirst().orElse(new MigrationInfo());

									// We may have located a new migration. Lets add it to the list
									if (!response.getMigrations().contains(currentInfo)) {
										response.getMigrations().add(currentInfo);
									}

									// String key = String.valueOf(getStartTime());
									// obj.put(key, info);
									//
									// // // TODO TESTCODE
									// // for (int i = 0; i < 30; i++) {
									// // obj.put(String.valueOf(i), info);
									// // }
									//
									purgeOldEntries(response.getMigrations());
									map.put("data", response, ph -> {
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
			MigrationStatusResponse response = (MigrationStatusResponse) map.get("data");
			if (response == null) {
				response = new MigrationStatusResponse();
			}

			MigrationInfo currentInfo = response.getMigrations().stream().filter(e -> startDate.equals(e.getStartDate())).findFirst().orElse(null);

			// We need to add the new migration to the list
			if (currentInfo != null) {
				response.getMigrations().remove(currentInfo);
			}
			response.getMigrations().add(currentInfo);

			purgeOldEntries(response.getMigrations());
			if (map.containsKey("data")) {
				map.replace("data", response);
			} else {
				map.put("data", response);
			}
		}
		return this;
	}

	public void purgeOldEntries(List<MigrationInfo> list) {
		Set<MigrationInfo> oldInfos = new HashSet<>();
		list.stream().sorted((o1, o2) -> {
			Long key1 = Long.valueOf(DateUtils.fromISO8601(o1.getStartDate()));
			Long key2 = Long.valueOf(DateUtils.fromISO8601(o2.getStartDate()));
			return Long.compare(key1, key2);
		}).skip(MAX_MIGRATION_DATE_ENTRIES).map(e -> {
			if (log.isDebugEnabled()) {
				log.debug("Removed info with date {" + e.getStartDate() + "} from object.");
			}
			oldInfos.add(e);
			return null;
		});
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
		this.status = COMPLETED_STATUS;
		updateStatus();
		JsonObject result = new JsonObject().put("type", "completed");
		message.reply(result);
		vertx.eventBus().publish(MESH_MIGRATION, result);
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
	 * @param message
	 * @param error
	 * @param failureMessage
	 */
	public MigrationStatusHandler handleError(Throwable error, String failureMessage) {
		log.error("Error handling migration", error);
		this.status = ERROR_STATUS;
		updateStatus();
		message.fail(100, failureMessage);
		vertx.eventBus().publish(MESH_MIGRATION, new JsonObject().put("type", "failed"));
		return this;
	}

}
