package com.gentics.mesh.core.data.schema;

import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerComparator;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.VersionUtil;

public interface HibFieldSchemaVersionElement<R extends FieldSchemaContainer, RM extends FieldSchemaContainerVersion, SC extends HibFieldSchemaElement<R, RM, SC, SCV>, SCV extends HibFieldSchemaVersionElement<R, RM, SC, SCV>>
	extends HibCoreElement<R>, Comparable<SCV> {

	/**
	 * Return the schema name.
	 * 
	 * @return
	 */
	String getName();

	/**
	 * Set the schema name.
	 * 
	 * @param name
	 */
	void setName(String name);

	/**
	 * Return the schema JSON.
	 * 
	 * @return
	 */
	String getJson();

	/**
	 * Set the schema JSON.
	 * 
	 * @param json
	 */
	void setJson(String json);

	/**
	 * Return the schema version.
	 * 
	 * @return
	 */
	String getVersion();

	/**
	 * Return the stored schema rest model.
	 * 
	 * @return
	 */
	RM getSchema();

	/**
	 * Set the schema REST model.
	 * 
	 * @param schema
	 */
	void setSchema(RM schema);

	/**
	 * Delete the version.
	 */
	void deleteElement();

	// Version chain

	/**
	 * Set the previous schema version.
	 * 
	 * @param version
	 */
	void setPreviousVersion(SCV version);

	/**
	 * Set the next schema version.
	 * 
	 * @param version
	 */
	void setNextVersion(SCV version);

	// Changes

	/**
	 * Return the next version of the change. This only applies when this version has a next version.
	 * 
	 * @return Next change or null when this is the current schema version which no further versions
	 */
	HibSchemaChange<?> getNextChange();

	/**
	 * Return the previous change of the version.
	 * 
	 * @return
	 */
	HibSchemaChange<?> getPreviousChange();

	/**
	 * Set the previous change that is linked to the version.
	 * 
	 * @param change
	 */
	void setPreviousChange(HibSchemaChange<?> change);

	/**
	 * Set the next change that is linked to the version.
	 * 
	 * @param change
	 */
	void setNextChange(HibSchemaChange<?> change);

	SCV getPreviousVersion();

	SCV getNextVersion();

	/**
	 * Return the parent schema container of the version.
	 *
	 * @return
	 */
	SC getSchemaContainer();

	/**
	 * Set the parent schema container of this version.
	 *
	 * @param container
	 */
	void setSchemaContainer(SC container);

	/**
	 * Set the version.
	 * 
	 * @param version
	 */
	void setVersion(String version);

	/**
	 * Returns a stream of all previous versions.
	 * 
	 * @return
	 */
	default Stream<SCV> getPreviousVersions() {
		return StreamUtil.untilNull(
			this::getPreviousVersion,
			HibFieldSchemaVersionElement::getPreviousVersion);
	}

	/**
	 * Generate a schema change list by comparing the schema with the specified schema update model which is extracted from the action context.
	 *
	 * @param ac
	 *            Action context that provides the schema update request
	 * @param comparator
	 *            Comparator to be used to compare the schemas
	 * @param restModel
	 *            Rest model of the container that should be compared
	 * @return Rest model which contains the changes list
	 */
	SchemaChangesListModel diff(InternalActionContext ac, FieldSchemaContainerComparator<?> comparator, FieldSchemaContainer restModel);

	/**
	 * Apply changes which will be extracted from the action context.
	 *
	 * @param ac
	 *            Action context that provides the migration request data
	 * @param batch
	 * @return The created schema container version
	 */
	SCV applyChanges(InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Apply the given list of changes to the schema container. This method will invoke the schema migration process.
	 *
	 * @param ac
	 * @param listOfChanges
	 * @param batch
	 * @return The created schema container version
	 */
	SCV applyChanges(InternalActionContext ac, SchemaChangesListModel listOfChanges, EventQueueBatch batch);

	/**
	 * Get the branches to which the container was assigned.
	 *
	 * @return Found branches of this version
	 */
	Result<? extends HibBranch> getBranches();

	/**
	 * Return an iterable of all jobs which reference the version via the _to_ reference.
	 *
	 * @return
	 */
	Iterable<? extends HibJob> referencedJobsViaTo();

	/**
	 * Return an iterable of all jobs which reference the version via the _from_ reference.
	 *
	 * @return
	 */
	Iterable<? extends HibJob> referencedJobsViaFrom();

	default int compareTo(SCV version) {
		return VersionUtil.compareVersions(getVersion(), version.getVersion());
	}

	/**
	 * Retrieves all changes for the next version.
	 *
	 * @return
	 */
	default Stream<HibSchemaChange<FieldSchemaContainer>> getChanges() {
		return StreamUtil.untilNull(
			() -> (HibSchemaChange<FieldSchemaContainer>) getNextChange(),
			change -> (HibSchemaChange<FieldSchemaContainer>) change.getNextChange());
	}
}
