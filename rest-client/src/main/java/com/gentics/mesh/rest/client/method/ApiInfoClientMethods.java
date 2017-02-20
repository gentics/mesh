package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.rest.client.MeshRequest;

public interface ApiInfoClientMethods {

	/**
	 * Load the mesh server API Info
	 * 
	 * @return
	 */
	MeshRequest<MeshServerInfoModel> getApiInfo();

	/**
	 * Load the mesh server RAML
	 * 
	 * @return
	 */
	MeshRequest<String> getRAML();
}
