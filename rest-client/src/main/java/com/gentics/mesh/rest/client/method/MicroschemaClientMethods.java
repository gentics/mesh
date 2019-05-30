package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

public interface MicroschemaClientMethods {

	/**
	 * Create a new microschema using the given request.
	 * 
	 * @param request
	 *            create request
	 * @return future for the microschema response
	 */
	MeshRequest<MicroschemaResponse> createMicroschema(MicroschemaCreateRequest request);

	/**
	 * Create a new microschema using the given uuid and request.
	 * 
	 * @param uuid
	 *            Uuid of the microschema
	 * @param request
	 * @return
	 */
	MeshRequest<MicroschemaResponse> createMicroschema(String uuid, MicroschemaCreateRequest request);

	/**
	 * Load the microschema with the given UUID.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<MicroschemaResponse> findMicroschemaByUuid(String uuid, ParameterProvider... parameters);

	/**
	 * Update the microschema with the given request.
	 * 
	 * @param uuid
	 *            Microschema UUID
	 * @param request
	 *            Update request
	 * @param parameters
	 * @return
	 */
	MeshRequest<GenericMessageResponse> updateMicroschema(String uuid, MicroschemaUpdateRequest request, ParameterProvider... parameters);

	/**
	 * Delete the given microschema.
	 *
	 * @param uuid Microschema UUID
	 * @return
	 */
	MeshRequest<EmptyResponse> deleteMicroschema(String uuid);

	/**
	 * Apply the given set of changes to the microschema.
	 * 
	 * @param uuid
	 *            Microschema UUID
	 * @param changes
	 * @return
	 */
	MeshRequest<GenericMessageResponse> applyChangesToMicroschema(String uuid, SchemaChangesListModel changes);

	/**
	 * Compare the given microschema with a currently stored one and return a list of changes.
	 * 
	 * @param uuid
	 * @param request
	 * @return
	 */
	MeshRequest<SchemaChangesListModel> diffMicroschema(String uuid, Microschema request);

}
