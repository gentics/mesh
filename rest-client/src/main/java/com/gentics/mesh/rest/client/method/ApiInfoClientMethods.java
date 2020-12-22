package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.monitoring.MonitoringRestClient;

/**
 * API info (/api/v1) client methods.
 */
public interface ApiInfoClientMethods {

	/**
	 * Load the mesh server API Info
	 * 
	 * @return
	 * @deprecated Use {@link MonitoringRestClient#versions()} instead.
	 */
	@Deprecated
	MeshRequest<MeshServerInfoModel> getApiInfo();

	/**
	 * Load the mesh server RAML
	 * 
	 * @return
	 */
	MeshRequest<String> getRAML();
}
