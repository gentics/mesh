package com.gentics.mesh.core.data.job;

import static com.gentics.mesh.core.rest.job.JobStatus.STARTING;

import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;

import io.reactivex.Completable;

/**
 * The core functionality interface for a {@link Job}.
 * 
 * @author plyhun
 *
 */
public interface JobCore extends Job {

	@Override
	default User getCreator() {
		return CommonTx.get().data().mesh().userProperties().getCreator(this);
	}
}
