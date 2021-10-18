package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.Map;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerMutator;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;

/**
 * DAO for schema-based container elements.
 * 
 * @author plyhun
 *
 * @param <R> REST model type
 * @param <RM> version model type
 * @param <SC> entity type
 * @param <SCV> entity version model type
 * @param <M> schema model type
 */
public interface ContainerDao<
		R extends FieldSchemaContainer, 
		RM extends FieldSchemaContainerVersion, 
		RE extends NameUuidReference<RE>, 
		SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>, 
		SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>,
		M extends FieldSchemaContainer
	> extends DaoGlobal<SC>, DaoTransformable<SC, R> {

	/**
	 * Delete the schema version, notifying context if necessary.
	 * 
	 * @param version
	 * @param bac
	 */
	void deleteVersion(SCV version, BulkActionContext bac);

	/**
	 * Load the schema version via the schema and version.
	 * 
	 * @param schema
	 * @param version
	 * @return
	 */
	SCV findVersionByRev(SC schema, String version);

	/**
	 * Return the schema version.
	 * 
	 * @param container
	 * @param versionUuid
	 * @return
	 */
	SCV findVersionByUuid(SC container, String versionUuid);

	/**
	 * Get the schema comparator for this container type.
	 * 
	 * @return
	 */
	FieldSchemaContainerComparator<M> getFieldSchemaContainerComparator();

	/**
	 * Diff the schema version with the request model and return a list of changes.
	 * 
	 * @param latestVersion
	 * @param ac
	 * @param requestModel
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	default SchemaChangesListModel diff(SCV latestVersion, InternalActionContext ac, M requestModel) {
		SchemaChangesListModel list = new SchemaChangesListModel();
		requestModel.validate();
		list.getChanges().addAll(((FieldSchemaContainerComparator) getFieldSchemaContainerComparator())
				.diff(latestVersion.transformToRestSync(ac, 0), requestModel));
		return list;
	};

	/**
	 * Find all schema versions for the given branch.
	 * 
	 * @param branch
	 * @return
	 */
	Result<SCV> findActiveSchemaVersions(HibBranch branch);

	/**
	 * Load the contents that use the given schema version for the given branch.
	 * 
	 * @param version
	 * @param branchUuid
	 * @return
	 */
	Result<? extends HibNodeFieldContainer> findDraftFieldContainers(SCV version, String branchUuid);

	/**
	 * Find all versions for the given schema.
	 * 
	 * @param schema
	 * @return
	 */
	Iterable<? extends SCV> findAllVersions(SC schema);

	/**
	 * Find all branches which reference the schema.
	 * 
	 * @param schema
	 * @return
	 */
	Map<HibBranch, SCV> findReferencedBranches(SC schema);

	/**
	 * Check whether the schema is linked to the project.
	 * 
	 * @param schema
	 * @param project
	 * @return
	 */
	boolean isLinkedToProject(SC schema, HibProject project);

	/**
	 * Unlink the schema from the project.
	 * 
	 * @param schema
	 * @param project
	 * @param batch
	 */
	void unlink(SC schema, HibProject project, EventQueueBatch batch);

	/**
	 * Apply changes to the schema.
	 * 
	 * @param version
	 * @param ac
	 * @param model
	 * @param batch
	 * @return
	 */
	default SCV applyChanges(SCV version, InternalActionContext ac, SchemaChangesListModel listOfChanges, EventQueueBatch batch) {
		if (listOfChanges.getChanges().isEmpty()) {
			throw error(BAD_REQUEST, "schema_migration_no_changes_specified");
		}
		HibSchemaChange<?> current = null;
		for (SchemaChangeModel restChange : listOfChanges.getChanges()) {
			HibSchemaChange<?> graphChange = createChange(version, restChange);
			// Set the first change to the schema container and chain all other changes to that change.
			if (current == null) {
				current = graphChange;
				version.setNextChange(current);
			} else {
				current.setNextChange(graphChange);
				current = graphChange;
			}
		}

		RM resultingSchema = new FieldSchemaContainerMutator().apply(version);
		resultingSchema.validate();

		// Increment version of the schema
		resultingSchema.setVersion(String.valueOf(Double.valueOf(resultingSchema.getVersion()) + 1));

		// Create and set the next version of the schema
		SCV nextVersion = Tx.get().create(version.getContainerVersionClass());
		nextVersion.setSchema(resultingSchema);

		// Check for conflicting container names
		String newName = resultingSchema.getName();
		SC foundContainer = findByName(resultingSchema.getName());
		if (foundContainer != null && !foundContainer.getUuid().equals(version.getSchemaContainer().getUuid())) {
			throw conflict(foundContainer.getUuid(), newName, "schema_conflicting_name", newName);
		}

		nextVersion.setSchemaContainer(version.getSchemaContainer());
		nextVersion.setName(resultingSchema.getName());
		version.getSchemaContainer().setName(resultingSchema.getName());
		version.setNextVersion(nextVersion);

		// Update the latest version of the schema container
		version.getSchemaContainer().setLatestVersion(nextVersion);

		// Update the search index
		batch.add(version.getSchemaContainer().onUpdated());
		return nextVersion;
	}

	/**
	 * Create schema change entity, based on the given model, .
	 * @param version 
	 * 
	 * @param version
	 * @param restChange
	 * @return
	 */
	HibSchemaChange<?> createChange(SCV version, SchemaChangeModel restChange);

	/**
	 * Apply changes to the schema version.
	 * 
	 * @param version
	 * @param ac
	 *            Action context which contains the changes payload
	 * @param batch
	 * @return
	 */
	default SCV applyChanges(SCV version, InternalActionContext ac, EventQueueBatch batch) {
		SchemaChangesListModel listOfChanges = JsonUtil.readValue(ac.getBodyAsString(), SchemaChangesListModel.class);

		if (version.getNextChange() != null) {
			throw error(INTERNAL_SERVER_ERROR, "migration_error_version_already_contains_changes", String.valueOf(version.getVersion()), version.getName());
		}
		return applyChanges(version, ac, listOfChanges, batch);
	}

	/**
	 * Delete the schema change, notifying the context, if necessary.
	 * 
	 * @param change
	 * @param bac
	 */
	default void deleteChange(HibSchemaChange<? extends FieldSchemaContainer> change, BulkActionContext bc) {
		HibSchemaChange<?> next = change.getNextChange();
		if (next != null) {
			deleteChange(next, bc);
		}
		Tx.get().delete(change, change.getClass());
	}
}
