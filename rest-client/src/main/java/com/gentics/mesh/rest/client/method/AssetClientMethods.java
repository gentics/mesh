package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.asset.AssetListResponse;
import com.gentics.mesh.core.rest.asset.AssetResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

import io.vertx.core.buffer.Buffer;

/**
 * Interface for asset specific REST API methods.
 */
public interface AssetClientMethods {

	/**
	 * Find the asset with the given UUID in the project with the given name.
	 * 
	 * @param projectName
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<AssetResponse> findAssetByUuid(String projectName, String uuid, ParameterProvider... parameters);

	/**
	 * Create a new asset.
	 * 
	 * @param projectName
	 * @param uuid
	 *            Uuid that will be used for the new asset
	 * @return
	 */
	MeshRequest<AssetResponse> createAsset(String projectName, String uuid);

	/**
	 * Update the node with the given UUID.
	 * 
	 * @param projectName
	 * @param uuid Uuid of the asset
	 * @param nodeUpdateRequest
	 * @param parameters
	 * @return
	 */
	MeshRequest<NodeResponse> updateAsset(String projectName, String uuid, Buffer fileData, String fileName, String contentType, ParameterProvider... parameters);

	/**
	 * Delete the node with the given UUID. All languages will be deleted.
	 * 
	 * @param projectName
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<Void> deleteAsset(String projectName, String uuid, ParameterProvider... parameters);

	/**
	 * Find all nodes within the project with the given name. The query parameters can be used to set paging and language settings.
	 * 
	 * @param projectName
	 * @param parameters
	 * @return
	 */
	MeshRequest<AssetListResponse> findAssets(String projectName, ParameterProvider... parameters);

}
