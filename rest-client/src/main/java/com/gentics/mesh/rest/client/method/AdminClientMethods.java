package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.rest.client.MeshRequest;

public interface AdminClientMethods {

	/**
	 * Return the mesh status.
	 * 
	 * @return
	 */
	MeshRequest<String> meshStatus();

	/**
	 * Return the current schema/microschema migration status.
	 * 
	 * @return
	 */
	MeshRequest<GenericMessageResponse> schemaMigrationStatus();

}
