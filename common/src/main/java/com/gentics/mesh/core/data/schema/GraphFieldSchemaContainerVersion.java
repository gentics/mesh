package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.ReferenceableElement;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.schema.handler.FieldSchemaContainerComparator;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madlmigration.TraversalResult;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.VersionUtil;

import java.util.stream.Stream;

/**
 * A {@link GraphFieldSchemaContainerVersion} stores the versioned data for a {@link GraphFieldSchemaContainer} element.
 * 
 * @param <R>
 *            Rest model response type
 * @param <RM><
 *            Rest model type
 * @param <RE>
 *            Reference model type
 * @param <SCV>
 *            Schema container version type
 * @param <SC>
 *            Schema container type
 * 
 */
public interface GraphFieldSchemaContainerVersion<R extends FieldSchemaContainer, RM extends FieldSchemaContainer, RE extends NameUuidReference<RE>, SCV extends GraphFieldSchemaContainerVersion<R, RM, RE, SCV, SC>, SC extends GraphFieldSchemaContainer<R, RE, SC, SCV>>
		extends MeshCoreVertex<R, SCV>, ReferenceableElement<RE>, Comparable<SCV> {

	public static final String VERSION_PROPERTY_KEY = "version";

	/**
	 * Return the schema version.
	 * 
	 * @return
	 */
	String getVersion();

	/**
	 * Set the version.
	 * 
	 * @param version
	 */
	void setVersion(String version);

	/**
	 * Return the schema model that is stored within the container.
	 * 
	 * @return
	 */
	RM getSchema();

	/**
	 * Set the schema model for the container.
	 * 
	 * @param schema
	 */
	void setSchema(RM schema);

	/**
	 * Return the change for the previous version of the schema. Normally the previous change was used to build the schema.
	 * 
	 * @return
	 */
	SchemaChange<?> getPreviousChange();

	/**
	 * Return the change for the next version.
	 * 
	 * @return Can be null if no further changes exist
	 */
	SchemaChange<?> getNextChange();

	default Stream<SchemaChange<FieldSchemaContainer>> getChanges() {
		return StreamUtil.untilNull(
			() -> (SchemaChange<FieldSchemaContainer>)getNextChange(),
			change -> (SchemaChange<FieldSchemaContainer>)change.getNextChange()
		);
	}

	/**
	 * Set the next change for the schema. The next change is the first change in the chain of changes that lead to the new schema version.
	 * 
	 * @param change
	 */
	void setNextChange(SchemaChange<?> change);

	/**
	 * Set the previous change for the schema. The previous change is the last change in the chain of changes that was used to create the schema container.
	 * 
	 * @param change
	 */
	void setPreviousChange(SchemaChange<?> change);

	/**
	 * Return the next version of this schema.
	 * 
	 * @return
	 */
	SCV getNextVersion();

	/**
	 * Set the next version of the schema container.
	 * 
	 * @param container
	 */
	void setNextVersion(SCV container);

	/**
	 * Return the previous version of this schema.
	 * 
	 * @return
	 */
	SCV getPreviousVersion();

	/**
	 * Set the previous version of the container.
	 * 
	 * @param container
	 */
	void setPreviousVersion(SCV container);

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
	 * Get the branches to which the container was assigned.
	 * 
	 * @return Found branches of this version
	 */
	TraversalResult<? extends Branch> getBranches();

	/**
	 * Load the stored schema JSON data.
	 * 
	 * @return
	 */
	String getJson();

	/**
	 * Update the stored schema JSON data.
	 * 
	 * @param json
	 */
	void setJson(String json);

	/**
	 * Compare two versions.
	 * 
	 * @param version
	 */
	default int compareTo(SCV version) {
		return VersionUtil.compareVersions(getVersion(), version.getVersion());
	}

	/**
	 * Return an iterable of all jobs which reference the version via the _to_ reference.
	 * 
	 * @return
	 */
	Iterable<Job> referencedJobsViaTo();

	/**
	 * Return an iterable of all jobs which reference the version via the _from_ reference.
	 * 
	 * @return
	 */
	Iterable<Job> referencedJobsViaFrom();
}
