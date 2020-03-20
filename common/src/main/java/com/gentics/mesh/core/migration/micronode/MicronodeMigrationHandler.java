package com.gentics.mesh.core.migration.micronode;

import com.gentics.mesh.context.MicronodeMigrationContext;

import io.reactivex.Completable;

public interface MicronodeMigrationHandler {

	/**
	 * Migrate all micronodes referencing the given microschema container to the latest version
	 * 
	 * @param context
	 * @return Completable which will be completed once the migration has completed
	 */
	Completable migrateMicronodes(MicronodeMigrationContext context);

}
