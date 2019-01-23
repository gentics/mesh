package com.gentics.mesh.core.endpoint.migration.impl;

import static com.gentics.mesh.MeshEvent.MESH_MIGRATION;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.COMPLETED;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.FAILED;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.gentics.mesh.core.data.branch.BranchVersionEdge;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.admin.migration.MigrationStatus;
import com.gentics.mesh.core.rest.admin.migration.MigrationType;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The migration status class keeps track of the status of a migration and manages also the errors and event handling.
 */
public class MigrationStatusHandlerImpl implements MigrationStatusHandler {

	private static final Logger log = LoggerFactory.getLogger(MigrationStatusHandlerImpl.class);

	private MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

	private Vertx vertx;

	private BranchVersionEdge versionEdge;

	private Job job;

	private long completionCount = 0;

	private MigrationStatus status;

	public MigrationStatusHandlerImpl(Job job, Vertx vertx, MigrationType type) {
		this.vertx = vertx;
		this.job = job;
	}

	@Override
	public MigrationStatusHandler commit() {
		// Load the status if it has not yet been set or loaded.
		if (status == null) {
			status = job.getStatus();
		}
		if (versionEdge != null) {
			versionEdge.setMigrationStatus(status);
		}
		job.setCompletionCount(completionCount);
		job.setStatus(status);

		Tx.getActive().getGraph().commit();
		return this;

	}

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
	 * <li>Update the job and potential version edge</li>
	 * </ul>
	 */
	public MigrationStatusHandler done() {
		setStatus(COMPLETED);
		log.info("Migration completed without errors.");
		JsonObject result = new JsonObject().put("type", "completed");
		vertx.eventBus().publish(MESH_MIGRATION.address, result);
		job.setStopTimestamp();
		commit();
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
		setStatus(FAILED);
		log.error("Error handling migration", error);

		vertx.eventBus().publish(MESH_MIGRATION.address, new JsonObject().put("type", status.name()));
		job.setStopTimestamp();
		job.setError(error);
		commit();
		return this;
	}

	@Override
	public void setVersionEdge(BranchVersionEdge versionEdge) {
		this.versionEdge = versionEdge;
	}

	@Override
	public void setCompletionCount(long completionCount) {
		this.completionCount = completionCount;
	}

	@Override
	public void setStatus(MigrationStatus status) {
		this.status = status;
	}

	@Override
	public void incCompleted() {
		completionCount++;
	}

}
