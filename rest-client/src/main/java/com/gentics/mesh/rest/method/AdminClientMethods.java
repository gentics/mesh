package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;

import io.vertx.core.Future;

public interface AdminClientMethods {

	/**
	 * Return the mesh status.
	 * 
	 * @return
	 */
	Future<String> meshStatus();

	/**
	 * Return the current schema/microschema migration status.
	 * 
	 * @return
	 */
	Future<GenericMessageResponse> schemaMigrationStatus();

}
