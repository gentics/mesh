package com.gentics.mesh.core.data.schema;

import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibReferenceableElement;
import com.gentics.mesh.core.data.job.HibJob;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.NameUuidReference;
import com.gentics.mesh.core.rest.event.branch.AbstractBranchAssignEventModel;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainerVersion;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.VersionUtil;

public interface HibFieldSchemaVersionElement<
			R extends FieldSchemaContainer, 
			RM extends FieldSchemaContainerVersion, 
			RE extends NameUuidReference<RE>, 
			SC extends HibFieldSchemaElement<R, RM, RE, SC, SCV>, 
			SCV extends HibFieldSchemaVersionElement<R, RM, RE, SC, SCV>>
	extends HibCoreElement<R>, HibReferenceableElement<RE>, Comparable<SCV> {

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
	 * Return the 'exclude from indexing' flag, or null.
	 * 
	 * @return
	 */
	Boolean getNoIndex();

	/**
	 * Set the 'exclude from indexing' flag.
	 * 
	 * @param noIndex
	 */
	void setNoIndex(Boolean noIndex);

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

	/**
	 * Get the version prior to this one, or null.
	 * 
	 * @return
	 */
	SCV getPreviousVersion();

	/**
	 * Get the version following this one, or null.
	 * 
	 * @return
	 */
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
	 * Get an event type to trigger on assigned to a branch.
	 * 
	 * @return
	 */
	MeshEvent getBranchAssignEvent();


	/**
	 * Get an event type to trigger on unassigned from a branch.
	 * 
	 * @return
	 */
	MeshEvent getBranchUnassignEvent();

	/**
	 * Get branch assignment event model class.
	 * 
	 * @return
	 */
	Class<? extends AbstractBranchAssignEventModel<RE>> getBranchAssignEventModelClass();

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

	default int compareTo(SCV version) {
		return VersionUtil.compareVersions(getVersion(), version.getVersion());
	}

	/**
	 * Retrieves all changes for the next version.
	 *
	 * @return
	 */
	@SuppressWarnings("unchecked")
	default Stream<HibSchemaChange<FieldSchemaContainer>> getChanges() {
		return StreamUtil.untilNull(
			() -> (HibSchemaChange<FieldSchemaContainer>) getNextChange(),
			change -> (HibSchemaChange<FieldSchemaContainer>) change.getNextChange());
	}
}
