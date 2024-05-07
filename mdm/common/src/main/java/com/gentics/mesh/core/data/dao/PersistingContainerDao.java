package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.event.Assignment.UNASSIGNED;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerComparator;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerMutator;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.event.branch.AbstractBranchAssignEventModel;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;

public interface PersistingContainerDao<
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			RE extends NameUuidReference<RE>, 
			SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>, 
			SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>, 
			M extends FieldSchemaContainer
		> extends PersistingDaoGlobal<SC>, ContainerDao<R, RM, RE, SC, SCV, M>, PersistingNamedEntityDao<SC> {

	/**
	 * Get the final type of the version persistence entity of the dao.
	 * 
	 * @return
	 */
	Class<? extends SCV> getVersionPersistenceClass();

	/**
	 * Create the corresponding persisted instance of the schema change operation for the given schema version.
	 * 
	 * @param version
	 * @param schemaChangeOperation
	 * @return
	 */
	HibSchemaChange<?> createPersistedChange(SCV version, SchemaChangeOperation schemaChangeOperation);

	/**
	 * Create new persisted version entity for the container entity.
	 * 
	 * @return
	 */
	default SCV createPersistedVersion(SC container, Consumer<SCV> inflater) {
		SCV version = (SCV) CommonTx.get().create(getVersionPersistenceClass());
		version.setSchemaContainer(container);
		inflater.accept(version);
		return version;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	default SchemaChangesListModel diff(SCV latestVersion, InternalActionContext ac, M requestModel) {
		SchemaChangesListModel list = new SchemaChangesListModel();
		requestModel.validate();
		list.getChanges().addAll(((FieldSchemaContainerComparator) getFieldSchemaContainerComparator())
				.diff(latestVersion.transformToRestSync(ac, 0), requestModel));
		return list;
	};

	@Override
	default Map<HibBranch, SCV> findReferencedBranches(SC schema) {
		return schema.findReferencedBranches();
	}

	@Override
	default SCV applyChanges(SCV version, InternalActionContext ac, SchemaChangesListModel listOfChanges,
			EventQueueBatch batch) {
		if (listOfChanges.getChanges().isEmpty()) {
			throw error(BAD_REQUEST, "schema_migration_no_changes_specified");
		}
		HibSchemaChange<?> current = null;
		for (SchemaChangeModel restChange : listOfChanges.getChanges()) {
			HibSchemaChange<?> graphChange = createChange(version, restChange);
			// Set the first change to the schema container and chain all other changes to
			// that change.
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
		SCV nextVersion = createPersistedVersion(version.getSchemaContainer(), v -> {
			v.setSchema(resultingSchema);
			
			// Check for conflicting container names
			String newName = resultingSchema.getName();
			SC foundContainer = findByName(resultingSchema.getName());
			if (foundContainer != null && !foundContainer.getUuid().equals(version.getSchemaContainer().getUuid())) {
				throw conflict(foundContainer.getUuid(), newName, "schema_conflicting_name", newName);
			}
			v.setSchemaContainer(version.getSchemaContainer());
			v.setName(resultingSchema.getName());
			v.setNoIndex(resultingSchema.getNoIndex());
		});
		
		version.getSchemaContainer().setName(resultingSchema.getName());
		version.setNextVersion(nextVersion);
		nextVersion.setPreviousVersion(version);

		// Update the latest version of the schema container
		version.getSchemaContainer().setLatestVersion(nextVersion);

		// Update the search index
		batch.add(version.getSchemaContainer().onUpdated());

		mergeIntoPersisted(version.getSchemaContainer());
		return nextVersion;
	}

	@Override
	default SCV applyChanges(SCV version, InternalActionContext ac, EventQueueBatch batch) {
		SchemaChangesListModel listOfChanges = JsonUtil.readValue(ac.getBodyAsString(), SchemaChangesListModel.class);

		if (version.getNextChange() != null) {
			throw error(INTERNAL_SERVER_ERROR, "migration_error_version_already_contains_changes",
					String.valueOf(version.getVersion()), version.getName());
		}
		return applyChanges(version, ac, listOfChanges, batch);
	}

	@Override
	default void deleteChange(HibSchemaChange<? extends FieldSchemaContainer> change, BulkActionContext bc) {
		HibSchemaChange<?> next = change.getNextChange();
		if (next != null) {
			deleteChange(next, bc);
		}
		CommonTx.get().delete(change);
	}

	@Override
	default HibSchemaChange<?> createChange(SCV version, SchemaChangeModel restChange) {
		// Create an instance
		HibSchemaChange<?> schemaChange = createPersistedChange(version, restChange.getOperation());
		
		// Set properties from rest model
		schemaChange.updateFromRest(restChange);
		return schemaChange;
	}

	@Override
	default void deleteVersion(SCV version, BulkActionContext bac) {
		CommonTx ctx = CommonTx.get();
		generateUnassignEvents(version).forEach(bac::add);
		// Delete change
		HibSchemaChange<?> change = version.getNextChange();
		if (change != null) {
			deleteChange(change, bac);
		}
		// Delete referenced jobs
		for (HibJob job : version.referencedJobsViaFrom()) {
			ctx.jobDao().delete(job, bac);
		}
		for (HibJob job : version.referencedJobsViaTo()) {
			ctx.jobDao().delete(job, bac);
		}
		beforeVersionDeletedFromDatabase(version);
		// Delete version
		ctx.delete(version);
	}

	/**
	 * Generates branch unassign events for every assigned branch.
	 * 
	 * @return
	 */
	private Stream<? extends AbstractBranchAssignEventModel<RE>> generateUnassignEvents(SCV version) {		
		return getBranches(version).stream()
			.map(branch -> branch.onContainerAssignEvent(version, UNASSIGNED, null, null, () -> {
				try {
					return version.getBranchAssignEventModelClass().getDeclaredConstructor().newInstance();
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
			}));
	}

	/**
	 * Method called before version is deleted
	 * @param version
	 */
	default void beforeVersionDeletedFromDatabase(SCV version) {}
}
