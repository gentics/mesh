package com.gentics.mesh.core.migration.impl;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.gentics.mesh.core.data.branch.HibBranchVersionAssignment;
import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.migration.MigrationStatusHandler;
import com.gentics.mesh.core.rest.job.JobStatus;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The migration status class keeps track of the status of a migration and manages also the errors and event handling.
 */
public class MigrationStatusHandlerImpl implements MigrationStatusHandler {

	private static final Logger log = LoggerFactory.getLogger(MigrationStatusHandlerImpl.class);

	private MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();

	private HibBranchVersionAssignment versionEdge;

	private long completionCount = 0;

	private JobStatus status;
	private final String jobUUID;

	public MigrationStatusHandlerImpl(String jobUUID) {
		this.jobUUID = jobUUID;
	}

	@Override
	public MigrationStatusHandler commit() {
		HibJob job = getJob();
		return commit(job);
	}

	private MigrationStatusHandler commit(HibJob job) {
		// Load the status if it has not yet been set or loaded.
		if (status == null) {
			status = job.getStatus();
		}
		if (versionEdge != null) {
			versionEdge = CommonTx.get().load(versionEdge.getId(), versionEdge.getClass());
			versionEdge.setMigrationStatus(status);
			CommonTx.get().persist(versionEdge, versionEdge.getClass());
		}
		job.setCompletionCount(completionCount);
		job.setStatus(status);

		CommonTx.get().jobDao().mergeIntoPersisted(job);
		Database db = CommonTx.get().data().mesh().database();
		db.tx().commit();
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
	 * <li>Update the job and potential version edge</li>
	 * </ul>
	 */
	public MigrationStatusHandler done() {
		HibJob job = getJob();
		setStatus(COMPLETED);
		log.info("Migration completed without errors.");
		job.setStopTimestamp();
		commit(job);
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
		HibJob job = getJob();
		setStatus(FAILED);
		log.error("Error handling migration", error);

		job.setStopTimestamp();
		job.setError(error);
		commit(job);
		return this;
	}

	@Override
	public void setVersionEdge(HibBranchVersionAssignment versionEdge) {
		this.versionEdge = versionEdge;
	}

	@Override
	public void setCompletionCount(long completionCount) {
		this.completionCount = completionCount;
	}

	@Override
	public void setStatus(JobStatus status) {
		this.status = status;
	}

	@Override
	public void incCompleted() {
		completionCount++;
	}

	private HibJob getJob() {
		Database db = CommonTx.get().data().mesh().database();
		JobDao jobDao = Tx.get().jobDao();
		return db.tx(() -> jobDao.findByUuid(jobUUID));
	}
}
