package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchListResponse;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.branch.info.BranchInfoMicroschemaList;
import com.gentics.mesh.core.rest.branch.info.BranchInfoSchemaList;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

/**
 * Interface for branch specific rest API methods
 */
public interface BranchClientMethods {

	/**
	 * Create a branch for the given project.
	 * 
	 * @param projectName
	 * @param branchCreateRequest
	 * @param parameters
	 * @return
	 */
	MeshRequest<BranchResponse> createBranch(String projectName, BranchCreateRequest branchCreateRequest, ParameterProvider... parameters);

	/**
	 * Create a branch for the given project using the provided uuid.
	 * 
	 * @param projectName
	 * @param uuid
	 * @param branchCreateRequest
	 * @param parameters
	 * @return
	 */
	MeshRequest<BranchResponse> createBranch(String projectName, String uuid, BranchCreateRequest branchCreateRequest,
			ParameterProvider... parameters);

	/**
	 * Find the branch with the given uuid in the project with the given name.
	 * 
	 * @param projectName
	 * @param branchUuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<BranchResponse> findBranchByUuid(String projectName, String branchUuid, ParameterProvider... parameters);

	/**
	 * Find all branches within the project with the given name. The query parameters can be used to set paging.
	 * 
	 * @param projectName
	 * @param parameters
	 * @return
	 */
	MeshRequest<BranchListResponse> findBranches(String projectName, ParameterProvider... parameters);

	/**
	 * Update the branch.
	 * 
	 * @param projectName
	 * @param branchUuid
	 * @param request
	 * @return
	 */
	MeshRequest<BranchResponse> updateBranch(String projectName, String branchUuid, BranchUpdateRequest request);

	/**
	 * Get schema versions assigned to a branch.
	 *
	 * @param projectName
	 * @param branchUuid
	 * @return
	 */
	MeshRequest<BranchInfoSchemaList> getBranchSchemaVersions(String projectName, String branchUuid);

	/**
	 * Assign the given schema versions to the branch.
	 * 
	 * @param projectName
	 * @param branchUuid
	 * @param schemaVersionReferences
	 * @return
	 */
	MeshRequest<BranchInfoSchemaList> assignBranchSchemaVersions(String projectName, String branchUuid, BranchInfoSchemaList schemaVersionReferences);

	/**
	 * Assign the given schema versions to the branch.
	 * 
	 * @param projectName
	 * @param branchUuid
	 * @param schemaVersionReferences
	 * @return
	 */
	MeshRequest<BranchInfoSchemaList> assignBranchSchemaVersions(String projectName, String branchUuid, SchemaReference... schemaVersionReferences);

	/**
	 * Get microschema versions assigned to a branch.
	 *
	 * @param projectName
	 * @param branchUuid
	 * @return
	 */
	MeshRequest<BranchInfoMicroschemaList> getBranchMicroschemaVersions(String projectName, String branchUuid);

	/**
	 * Assign the given microschema versions to the branch.
	 * 
	 * @param projectName
	 * @param branchUuid
	 * @param microschemaVersionReferences
	 * @return
	 */
	MeshRequest<BranchInfoMicroschemaList> assignBranchMicroschemaVersions(String projectName, String branchUuid,
			BranchInfoMicroschemaList microschemaVersionReferences);

	/**
	 * Assign the given microschema versions to the branch.
	 * 
	 * @param projectName
	 * @param branchUuid
	 * @param microschemaVersionReferences
	 * @return
	 */
	MeshRequest<BranchInfoMicroschemaList> assignBranchMicroschemaVersions(String projectName, String branchUuid,
			MicroschemaReference... microschemaVersionReferences);

	/**
	 * Invoke the node migration for not yet migrated nodes of schemas that are assigned to the branch.
	 * 
	 * @param projectName
	 * @param branchUuid
	 * @return
	 */
	MeshRequest<GenericMessageResponse> migrateBranchSchemas(String projectName, String branchUuid);

	/**
	 * Invoke the micronode migration for not yet migrated micronodes of microschemas that are assigned to the branch.
	 * 
	 * @param projectName
	 * @param branchUuid
	 * @return
	 */
	MeshRequest<GenericMessageResponse> migrateBranchMicroschemas(String projectName, String branchUuid);

	/**
	 * Set a branch to be the latest branch for the project.
	 * 
	 * @param projectName
	 * @param branchUuid
	 * @return
	 */
	MeshRequest<BranchResponse> setLatestBranch(String projectName, String branchUuid);
}
