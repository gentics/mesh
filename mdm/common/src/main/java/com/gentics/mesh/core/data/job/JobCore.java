package com.gentics.mesh.core.data.job;

import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;

import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;

import io.reactivex.Completable;

/**
 * The core functionality interface for a {@link HibJob}.
 * 
 * @author plyhun
 *
 */
public interface JobCore extends HibJob {

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
	 * Actual implementation of the task, executed by a job.
	 */
	Completable processTask(Database db);
}
