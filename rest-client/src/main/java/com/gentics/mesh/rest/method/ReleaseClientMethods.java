package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.core.rest.release.ReleaseListResponse;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.release.ReleaseUpdateRequest;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.MicroschemaReferenceList;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReferenceList;
import com.gentics.mesh.parameter.ParameterProvider;

import io.vertx.core.Future;

/**
 * Interface for Release specific rest API methods
 */
public interface ReleaseClientMethods {
	/**
	 * Create a release for the given project.
	 * 
	 * @param projectName
	 * @param releaseCreateRequest
	 * @param parameters
	 * @return
	 */
	Future<ReleaseResponse> createRelease(String projectName, ReleaseCreateRequest releaseCreateRequest, ParameterProvider... parameters);

	/**
	 * Find the release with the given uuid in the project with the given name.
	 * 
	 * @param projectName
	 * @param releaseUuid
	 * @param parameters
	 * @return
	 */
	Future<ReleaseResponse> findReleaseByUuid(String projectName, String releaseUuid, ParameterProvider... parameters);

	/**
	 * Find all releases within the project with the given name. The query parameters can be used to set paging.
	 * 
	 * @param projectName
	 * @param parameters
	 * @return
	 */
	Future<ReleaseListResponse> findReleases(String projectName, ParameterProvider... parameters);

	/**
	 * Update the release.
	 * 
	 * @param projectName
	 * @param releaseUuid
	 * @param request
	 * @return
	 */
	Future<ReleaseResponse> updateRelease(String projectName, String releaseUuid, ReleaseUpdateRequest request);

	/**
	 * Get schema versions assigned to a release
	 *
	 * @param projectName
	 * @param releaseUuid
	 * @return
	 */
	Future<SchemaReferenceList> getReleaseSchemaVersions(String projectName, String releaseUuid);

	/**
	 * Assign the given schema versions to the release
	 * @param projectName 
	 * @param releaseUuid
	 * @param schemaVersionReferences
	 * @return
	 */
	Future<SchemaReferenceList> assignReleaseSchemaVersions(String projectName, String releaseUuid, SchemaReferenceList schemaVersionReferences);

	/**
	 * Assign the given schema versions to the release
	 * @param projectName 
	 * @param releaseUuid
	 * @param schemaVersionReferences
	 * @return
	 */
	Future<SchemaReferenceList> assignReleaseSchemaVersions(String projectName, String releaseUuid, SchemaReference...schemaVersionReferences);

	/**
	 * Get microschema versions assigned to a release
	 *
	 * @param projectName
	 * @param releaseUuid
	 * @return
	 */
	Future<MicroschemaReferenceList> getReleaseMicroschemaVersions(String projectName, String releaseUuid);

	/**
	 * Assign the given microschema versions to the release
	 * @param projectName 
	 * @param releaseUuid
	 * @param microschemaVersionReferences
	 * @return
	 */
	Future<MicroschemaReferenceList> assignReleaseMicroschemaVersions(String projectName, String releaseUuid, MicroschemaReferenceList microschemaVersionReferences);

	/**
	 * Assign the given microschema versions to the release
	 * @param projectName 
	 * @param releaseUuid
	 * @param microschemaVersionReferences
	 * @return
	 */
	Future<MicroschemaReferenceList> assignReleaseMicroschemaVersions(String projectName, String releaseUuid, MicroschemaReference...microschemaVersionReferences);
}
