package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.node.NodeDownloadResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.impl.ImageManipulationParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;

import io.vertx.core.buffer.Buffer;

/**
 * Interface for Node Binary Field specific REST API methods.
 */
public interface NodeBinaryFieldClientMethods {

	/**
	 * Update the binary field for the node with the given nodeUuid in the given project using the provided data buffer.
	 * 
	 * @param projectName
	 *            Name of the project which contains the node
	 * @param nodeUuid
	 *            Uuid of the node
	 * @param languageTag
	 *            Language tag of the node
	 * @param nodeVersion
	 *            Node version
	 * @param fieldKey
	 *            Key of the field which holds the binary data
	 * @param fileData
	 *            Buffer that serves the binary data
	 * @param fileName
	 * @param contentType
	 * @return
	 */
	MeshRequest<NodeResponse> updateNodeBinaryField(String projectName, String nodeUuid, String languageTag, String nodeVersion,
			String fieldKey, Buffer fileData, String fileName, String contentType, ParameterProvider... parameters);

	/**
	 * Download the binary field of the given node in the given project.
	 * 
	 * @param projectName
	 * @param nodeUuid
	 * @param languageTag
	 * @param fieldKey
	 * @param parameters
	 * @return Mesh request which provides a download response that contains a reference to the byte buffer with the binary data
	 */
	MeshRequest<NodeDownloadResponse> downloadBinaryField(String projectName, String nodeUuid, String languageTag, String fieldKey,
			ParameterProvider... parameters);

	/**
	 * Transform the binary field of the given node in the given project
	 * 
	 * @param projectName
	 *            project name
	 * @param nodeUuid
	 *            uuid of hte node
	 * @param languageTag
	 *            language tag
	 * @param version
	 *            Node version
	 * @param fieldKey
	 *            field key
	 * @param imageManipulationParameter
	 *            parameters for the image transformation
	 * @return Mesh request
	 */
	MeshRequest<NodeResponse> transformNodeBinaryField(String projectName, String nodeUuid, String languageTag, String version,
			String fieldKey, ImageManipulationParametersImpl imageManipulationParameter);
}
