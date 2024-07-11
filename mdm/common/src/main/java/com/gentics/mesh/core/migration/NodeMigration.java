package com.gentics.mesh.core.migration;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.NodeMigrationActionContext;

import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.migration.SchemaMigrationMeshEventModel;
import com.gentics.mesh.core.rest.job.JobStatus;
import io.reactivex.Completable;

public interface NodeMigration {

	/**
	 * Prepare the migration action context
	 * @return
	 */
	NodeMigrationActionContextImpl prepareContext(Job job);

	/**
	 * Invoked inside a transaction after the context was prepared
	 * @param context
	 */
	default void afterContextPrepared(NodeMigrationActionContextImpl context) {}

	/**
	 * Migrate all nodes of a branch referencing the given schema container to the latest version of the schema.
	 *
	 * @param context
	 *            Migration context
	 * @return Completable which is completed once the migration finishes
	 */
	Completable migrateNodes(NodeMigrationActionContext context);

	/**
	 * Invoked inside a transaction before the container list is migrated.
	 * @param containerList
	 * @param ac
	 */
	default void beforeBatchMigration(List<? extends NodeFieldContainer> containerList, InternalActionContext ac) {}

	/**
	 * Purge the container list. Invoked inside the migration transaction.
	 * @param toPurge contains pair of a container with its nullable parent
	 *
	 */
	void bulkPurge(List<NodeFieldContainer> toPurge);

	/**
	 * Create a migration event model from the provided params
	 * @param job
	 * @param tx
	 * @param event
	 * @param status
	 * @return
	 */
	default SchemaMigrationMeshEventModel createEvent(Job job, Tx tx, MeshEvent event, JobStatus status) {
		SchemaMigrationMeshEventModel model = new SchemaMigrationMeshEventModel();
		model.setEvent(event);

		SchemaVersion toVersion = job.getToSchemaVersion();
		model.setToVersion(toVersion.transformToReference());

		SchemaVersion fromVersion = job.getFromSchemaVersion();
		model.setFromVersion(fromVersion.transformToReference());

		Branch branch = job.getBranch();
		Project project = branch.getProject();
		model.setProject(project.transformToReference());
		model.setBranch(branch.transformToReference());

		model.setOrigin(tx.data().options().getNodeName());
		model.setStatus(status);
		return model;
	}
}
