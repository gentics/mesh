package com.gentics.mesh.core.migration;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.MicronodeMigrationContext;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import io.reactivex.Completable;

public interface MicronodeMigration {

	/**
	 * Migrate all micronodes referencing the given microschema container to the latest version
	 * 
	 * @param context
	 * @return Completable which will be completed once the migration has completed
	 */
	Completable migrateMicronodes(MicronodeMigrationContext context);

	/**
	 * Called before a batch migration is executed
	 * @param containerList
	 * @param ac
	 */
	default void beforeBatchMigration(List<? extends HibNodeFieldContainer> containerList, InternalActionContext ac) {}
}
