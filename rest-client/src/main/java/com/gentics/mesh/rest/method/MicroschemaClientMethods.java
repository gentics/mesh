package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.schema.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaUpdateRequest;
import com.gentics.mesh.query.QueryParameterProvider;

import io.vertx.core.Future;

public interface MicroschemaClientMethods {
	/**
	 * Create a new microschema using the given request.
	 * 
	 * @param request create request
	 * @return future for the microschema response
	 */
	Future<MicroschemaResponse> createMicroschema(MicroschemaCreateRequest request);

	/**
	 * Load the microschema with the given uuid.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	Future<MicroschemaResponse> findMicroschemaByUuid(String uuid, QueryParameterProvider... parameters);

	/**
	 * Update the microschema with the given request.
	 * 
	 * @param uuid
	 *            Microschema uuid
	 * @param request
	 *            Update request
	 * @return
	 */
	Future<MicroschemaResponse> updateMicroschema(String uuid, MicroschemaUpdateRequest request);

	/**
	 * Delete the given microschema
	 * 
	 * @param uuid
	 *            Microschema uuid
	 * @return
	 */
	Future<GenericMessageResponse> deleteMicroschema(String uuid);

}
