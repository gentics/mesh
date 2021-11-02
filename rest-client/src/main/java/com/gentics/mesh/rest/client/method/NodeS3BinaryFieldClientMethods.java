package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryMetadataRequest;
import com.gentics.mesh.core.rest.node.field.s3binary.S3BinaryUploadRequest;
import com.gentics.mesh.core.rest.node.field.s3binary.S3RestResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

/**
 * Interface for Node S3Binary Field specific REST API methods.
 */
public interface NodeS3BinaryFieldClientMethods {

	/**
	 * Update the node adding a S3 Binary Field. Keep in mind that the real uploading uses the response from this call and uploads directly to AWS.
	 *
	 * @param projectName
	 * @param nodeUuid
	 * @param fieldKey
	 * @param request
	 * @param parameters
	 * @return
	 */
	MeshRequest<S3RestResponse> updateNodeS3BinaryField(String projectName, String nodeUuid, String fieldKey, S3BinaryUploadRequest request, ParameterProvider... parameters);
	/**
	 * Extracts the metadata for this S3 Binary Field
	 *
	 * @param projectName
	 * @param nodeUuid
	 * @param fieldKey
	 * @param request
	 * @param parameters
	 * @return
	 */
	MeshRequest<NodeResponse> extractMetadataNodeS3BinaryField(String projectName, String nodeUuid, String fieldKey, S3BinaryMetadataRequest request, ParameterProvider... parameters);
}
