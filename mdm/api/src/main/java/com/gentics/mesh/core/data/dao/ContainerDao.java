package com.gentics.mesh.core.data.dao;

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
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;

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
	 * Delete the schema change, notifying the context, if necessary.
	 * 
	 * @param change
	 * @param bac
	 */
	void deleteChange(HibSchemaChange<?> change, BulkActionContext bac);

	/**
	 * Delete the schema version, notifying context if necessary.
	 * 
	 * @param version
	 * @param bac
	 */
	void deleteVersion(SCV version, BulkActionContext bac);

	/**
	 * Find the version of the schema.
	 * 
	 * @param hibMicroschema
	 * @param version
	 * @return
	 */
	SCV findVersionByRev(SC schema, String version);

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
	default SCV applyChanges(SCV version, InternalActionContext ac, SchemaChangesListModel model, EventQueueBatch batch) {
		return version.applyChanges(ac, model, batch);
	}

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
		return version.applyChanges(ac, batch);
	}
}
