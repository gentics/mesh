package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.rest.MeshRequest;

public interface ApiInfoClientMethods {

	/**
	 * Load the mesh server API Info
	 * 
	 * @return
	 */
	MeshRequest<MeshServerInfoModel> getApiInfo();

}
