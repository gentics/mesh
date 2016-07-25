package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.MeshServerInfoModel;

import io.vertx.core.Future;

public interface ApiInfoClientMethods {

	/**
	 * Load the mesh server API Info
	 * 
	 * @return
	 */
	Future<MeshServerInfoModel> getApiInfo();

}
