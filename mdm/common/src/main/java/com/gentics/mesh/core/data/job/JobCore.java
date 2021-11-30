package com.gentics.mesh.core.data.job;

import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;

import org.apache.commons.lang3.exception.ExceptionUtils;

import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.job.JobStatus;

import io.reactivex.Completable;

public interface JobCore extends HibJob {

	@Override
	default void setError(Throwable e) {
		setErrorDetail(ExceptionUtils.getStackTrace(e));
		setErrorMessage(e.getMessage());
	}

	@Override
	default boolean hasFailed() {
		return getErrorMessage() != null || getErrorDetail() != null;
	}

	@Override
	default void markAsFailed(Exception e) {
		setError(e);
	}

	@Override
	default void resetJob() {
		setStartTimestamp(null);
		setStopTimestamp(null);
		setErrorDetail(null);
		setErrorMessage(null);
		setStatus(JobStatus.QUEUED);
	}

	@Override
	default HibUser getCreator() {
		return CommonTx.get().data().mesh().userProperties().getCreator(this);
	}

	@Override
	default Completable process() {
		Database db = CommonTx.get().data().mesh().database();
		return Completable.defer(() -> {

			db.tx(() -> {
				log.info("Processing job {" + getUuid() + "}");
				setStartTimestamp();
				setStatus(STARTING);
				setNodeName();
			});

			return processTask(db);
		});
	}

	/**
	 * Actual implementation of the task which the job executes.
	 */
	Completable processTask(Database db);
}
