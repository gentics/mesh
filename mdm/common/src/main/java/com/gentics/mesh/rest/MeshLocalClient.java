package com.gentics.mesh.rest;

import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;

/**
 * {@link MeshRestClient} for local embedded operation.
 */
public interface MeshLocalClient extends MeshRestClient {

	/**
	 * Set the user which is used for authentication.
	 *
	 * @param user
	 */
	void setUser(MeshAuthUser user);

	MeshRequest<NodeResponse> updateNodeBinaryField(String projectName, String nodeUuid, String languageTag, String version, String fieldKey,
		byte[] fileData, String fileName, String contentType, ParameterProvider... parameters);

}
