package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.core.rest.release.ReleaseListResponse;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.core.rest.release.ReleaseUpdateRequest;
import com.gentics.mesh.core.rest.release.info.ReleaseInfoMicroschemaList;
import com.gentics.mesh.core.rest.release.info.ReleaseInfoSchemaList;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

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
	MeshRequest<ReleaseResponse> createRelease(String projectName, ReleaseCreateRequest releaseCreateRequest, ParameterProvider... parameters);

	/**
	 * Create a release for the given project using the provided uuid.
	 * 
	 * @param projectName
	 * @param uuid
	 * @param releaseCreateRequest
	 * @param parameters
	 * @return
	 */
	MeshRequest<ReleaseResponse> createRelease(String projectName, String uuid, ReleaseCreateRequest releaseCreateRequest,
			ParameterProvider... parameters);

	/**
	 * Find the release with the given uuid in the project with the given name.
	 * 
	 * @param projectName
	 * @param releaseUuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<ReleaseResponse> findReleaseByUuid(String projectName, String releaseUuid, ParameterProvider... parameters);

	/**
	 * Find all releases within the project with the given name. The query parameters can be used to set paging.
	 * 
	 * @param projectName
	 * @param parameters
	 * @return
	 */
	MeshRequest<ReleaseListResponse> findReleases(String projectName, ParameterProvider... parameters);

	/**
	 * Update the release.
	 * 
	 * @param projectName
	 * @param releaseUuid
	 * @param request
	 * @return
	 */
	MeshRequest<ReleaseResponse> updateRelease(String projectName, String releaseUuid, ReleaseUpdateRequest request);

	/**
	 * Get schema versions assigned to a release.
	 *
	 * @param projectName
	 * @param releaseUuid
	 * @return
	 */
	MeshRequest<ReleaseInfoSchemaList> getReleaseSchemaVersions(String projectName, String releaseUuid);

	/**
	 * Assign the given schema versions to the release.
	 * 
	 * @param projectName
	 * @param releaseUuid
	 * @param schemaVersionReferences
	 * @return
	 */
	MeshRequest<ReleaseInfoSchemaList> assignReleaseSchemaVersions(String projectName, String releaseUuid, ReleaseInfoSchemaList schemaVersionReferences);

	/**
	 * Assign the given schema versions to the release.
	 * 
	 * @param projectName
	 * @param releaseUuid
	 * @param schemaVersionReferences
	 * @return
	 */
	MeshRequest<ReleaseInfoSchemaList> assignReleaseSchemaVersions(String projectName, String releaseUuid, SchemaReference... schemaVersionReferences);

	/**
	 * Get microschema versions assigned to a release.
	 *
	 * @param projectName
	 * @param releaseUuid
	 * @return
	 */
	MeshRequest<ReleaseInfoMicroschemaList> getReleaseMicroschemaVersions(String projectName, String releaseUuid);

	/**
	 * Assign the given microschema versions to the release
	 * 
	 * @param projectName
	 * @param releaseUuid
	 * @param microschemaVersionReferences
	 * @return
	 */
	MeshRequest<ReleaseInfoMicroschemaList> assignReleaseMicroschemaVersions(String projectName, String releaseUuid,
			ReleaseInfoMicroschemaList microschemaVersionReferences);

	/**
	 * Assign the given microschema versions to the release
	 * 
	 * @param projectName
	 * @param releaseUuid
	 * @param microschemaVersionReferences
	 * @return
	 */
	MeshRequest<ReleaseInfoMicroschemaList> assignReleaseMicroschemaVersions(String projectName, String releaseUuid,
			MicroschemaReference... microschemaVersionReferences);

	/**
	 * Invoke the node migration for not yet migrated nodes of schemas that are assigned to the release.
	 * 
	 * @param projectName
	 * @param releaseUuid
	 * @return
	 */
	MeshRequest<GenericMessageResponse> migrateReleaseSchemas(String projectName, String releaseUuid);

	/**
	 * Invoke the micronode migration for not yet migrated micronodes of microschemas that are assigned to the release.
	 * 
	 * @param projectName
	 * @param releaseUuid
	 * @return
	 */
	MeshRequest<GenericMessageResponse> migrateReleaseMicroschemas(String projectName, String releaseUuid);
}
