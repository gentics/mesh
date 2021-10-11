package com.gentics.mesh.core.data.schema;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

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
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.VersionUtil;

public interface HibFieldSchemaVersionElement<R extends FieldSchemaContainer, RM extends FieldSchemaContainerVersion, SC extends HibFieldSchemaElement<R, RM, SC, SCV>, SCV extends HibFieldSchemaVersionElement<R, RM, SC, SCV>>
	extends HibCoreElement<R>, Comparable<SCV> {

	/**
	 * Return the class that is used for container versions.
	 * 
	 * @return Class of the container version
	 */
	Class<? extends SCV> getContainerVersionClass();

	/**
	 * Return the class that is used for containers.
	 * 
	 * @return Class of the container
	 */
	Class<? extends SC> getContainerClass();

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

	/**
	 * Transform the element into the matching rest model response synchronously.
	 *
	 * @param ac
	 *            Context of the calling action
	 * @param level
	 *            Level of transformation
	 * @param languageTags
	 *            optional list of language tags to be used for language fallback
	 * @return
	 * @deprecated Use DAO method to transform elements instead
	 */
	R transformToRestSync(InternalActionContext ac, int level, String... languageTags);

	/**
	 * Apply the given list of changes to the schema container. This method will invoke the schema migration process.
	 *
	 * @param ac
	 * @param listOfChanges
	 * @param batch
	 * @return The created schema container version
	 */
	SCV applyChanges(InternalActionContext ac, SchemaChangesListModel listOfChanges, EventQueueBatch batch);

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

	default SchemaChangesListModel diff(InternalActionContext ac, FieldSchemaContainerComparator comparator,
		FieldSchemaContainer fieldContainerModel) {
		SchemaChangesListModel list = new SchemaChangesListModel();
		fieldContainerModel.validate();
		list.getChanges().addAll(comparator.diff(transformToRestSync(ac, 0), fieldContainerModel));
		return list;
	}

	/**
	 * Apply changes which will be extracted from the action context.
	 *
	 * @param ac
	 *            Action context that provides the migration request data
	 * @param batch
	 * @return The created schema container version
	 */
	default SCV applyChanges(InternalActionContext ac, EventQueueBatch batch) {
		SchemaChangesListModel listOfChanges = JsonUtil.readValue(ac.getBodyAsString(), SchemaChangesListModel.class);

		if (getNextChange() != null) {
			throw error(INTERNAL_SERVER_ERROR, "migration_error_version_already_contains_changes", String.valueOf(getVersion()), getName());
		}
		return applyChanges(ac, listOfChanges, batch);
	}
}
