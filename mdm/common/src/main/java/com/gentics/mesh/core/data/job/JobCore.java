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
}
