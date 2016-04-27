package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.core.data.root.ReleaseRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.release.ReleaseReference;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.util.InvalidArgumentException;

/**
 * Interface for Release Vertex
 */
public interface Release extends MeshCoreVertex<ReleaseResponse, Release>, NamedElement, ReferenceableElement<ReleaseReference>, UserTrackingVertex {
	public static final String TYPE = "release";

	/**
	 * Get whether the release is active
	 * @return true for active release
	 */
	boolean isActive();

	/**
	 * Set whether the release is active
	 * @param active true for active
	 */
	void setActive(boolean active);

	/**
	 * Get whether all nodes of the previous release have been migrated
	 * @return true iff all nodes have been migrated
	 */
	boolean isMigrated();

	/**
	 * Set whether all nodes have been migrated
	 * @param migrated true iff all nodes have been migrated
	 */
	void setMigrated(boolean migrated);

	/**
	 * Get the next Release
	 * @return next Release
	 */
	Release getNextRelease();

	/**
	 * Set the next Release
	 * @param release next Release
	 */
	void setNextRelease(Release release);

	/**
	 * Get the previous Release
	 * @return previous Release
	 */
	Release getPreviousRelease();

	/**
	 * Get the root vertex
	 * @return
	 */
	ReleaseRoot getRoot();

	/**
	 * Assign the given schema version to the release.
	 * Unassign all other schema versions of the schema
	 * @param schemaContainerVersion
	 */
	void assignSchemaVersion(SchemaContainerVersion schemaContainerVersion);

	/**
	 * Unassign all schema versions of the given schema from this release
	 * @param schemaContainer
	 */
	void unassignSchema(SchemaContainer schemaContainer);

	/**
	 * Check whether a version of this schema container is assigned to this release
	 *
	 * @param schema schema
	 * @return true iff assigned
	 */
	boolean contains(SchemaContainer schema);

	/**
	 * Check whether the given schema container version is assigned to this release
	 *
	 * @param schemaContainerVersion schema container version
	 * @return true iff assigned
	 */
	boolean contains(SchemaContainerVersion schemaContainerVersion);

	/**
	 * Get the schema container version of the given schema container, that is assigned to this release or null if not assigned at all
	 * @param schemaContainer schema container
	 * @return assigned version or null
	 */
	SchemaContainerVersion getVersion(SchemaContainer schemaContainer);

	/**
	 * Get list of all schema container versions
	 * @return list
	 * @throws InvalidArgumentException
	 */
	List<? extends SchemaContainerVersion> findAllSchemaVersions() throws InvalidArgumentException;

	/**
	 * Assign the given microschema version to the release
	 * Unassign all other versions of the microschema
	 * @param microschemaContainerVersion
	 */
	void assignMicroschemaVersion(MicroschemaContainerVersion microschemaContainerVersion);

	/**
	 * Unassign all versions of the given microschema from this release
	 * @param microschemaContainer
	 */
	void unassignMicroschema(MicroschemaContainer microschemaContainer);

	/**
	 * Check whether a version of this microschema container is assigned to this release
	 *
	 * @param microschema microschema
	 * @return true iff assigned
	 */
	boolean contains(MicroschemaContainer microschema);

	/**
	 * Check whether the given microschema container version is assigned to this release
	 *
	 * @param microschemaContainerVersion microschema container version
	 * @return true iff assigned
	 */
	boolean contains(MicroschemaContainerVersion microschemaContainerVersion);

	/**
	 * Get the microschema container version of the given microschema container, that is assigned to this release or null if not assigned at all
	 * @param microschemaContainer schema container
	 * @return assigned version or null
	 */
	MicroschemaContainerVersion getVersion(MicroschemaContainer microschemaContainer);

	/**
	 * Get list of all microschema container versions
	 * @return list
	 * @throws InvalidArgumentException
	 */
	List<? extends MicroschemaContainerVersion> findAllMicroschemaVersions() throws InvalidArgumentException;

}
