package com.gentics.mesh.core.project.maintenance;

import java.time.ZonedDateTime;

import com.gentics.mesh.core.data.project.HibProject;

import io.reactivex.Completable;

/**
 * Node version purge operation handler.
 */
public interface ProjectVersionPurgeHandler {

	/**
	 * Purge the versions of all nodes in the project.
	 * 
	 * @param project
	 * @param maxAge
	 *            Limit the purge operation to versions which exceed the max age.
	 * @return
	 */
	Completable purgeVersions(HibProject project, ZonedDateTime maxAge);

}
