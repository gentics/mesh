package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.s3binary.S3RestResponse;
import com.gentics.mesh.parameter.ImageManipulationParameters;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshBinaryResponse;
import com.gentics.mesh.rest.client.MeshRequest;

import java.io.InputStream;

/**
 * Interface for Node S3Binary Field specific REST API methods.
 */
public interface NodeS3BinaryFieldClientMethods {

	/**
	 * Update the node adding a S3 Binary Field. Keep in mind that the real uploading uses the response from this call and uploads directly to AWS.
	 *
	 * @param projectName
	 *            project name
	 * @param nodeUuid
	 *            UUID of the node
	 * @param fieldKey
	 *            field key
	 * @return Mesh request
	 */
	MeshRequest<S3RestResponse> updateNodeS3BinaryField(String projectName, String nodeUuid, String fieldKey, String body);
	/**
	 * Extracts the metadata for this S3 Binary Field
	 *
	 * @param projectName
	 *            project name
	 * @param nodeUuid
	 *            UUID of the node
	 * @param fieldKey
	 *            field key
	 * @return Mesh request
	 */
	MeshRequest<NodeResponse> extractMetadataNodeS3BinaryField(String projectName, String nodeUuid, String fieldKey, String body);
}
