package com.gentics.mesh.core.project.maintenance;

import java.time.ZonedDateTime;

import com.gentics.mesh.core.data.Project;

import io.reactivex.Completable;

public interface ProjectVersionPurgeHandler {

	/**
	 * Purge the versions of all nodes in the project.
	 * 
	 * @param project
	 * @param maxAge
	 *            Limit the purge operation to versions which exceed the max age.
	 * @return
	 */
	Completable purgeVersions(Project project, ZonedDateTime maxAge);

}
