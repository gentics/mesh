package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

public interface MicroschemaClientMethods {

	/**
	 * Create a new microschema using the given request.
	 * 
	 * @param request
	 *            create request
	 * @return future for the microschema response
	 */
	MeshRequest<Microschema> createMicroschema(Microschema request);

	/**
	 * Load the microschema with the given UUID.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<Microschema> findMicroschemaByUuid(String uuid, ParameterProvider... parameters);

	/**
	 * Update the microschema with the given request.
	 * 
	 * @param uuid
	 *            Microschema UUID
	 * @param request
	 *            Update request
	 * @return
	 */
	MeshRequest<GenericMessageResponse> updateMicroschema(String uuid, Microschema request);

	/**
	 * Delete the given microschema.
	 * 
	 * @param uuid
	 *            Microschema UUID
	 * @return
	 */
	MeshRequest<GenericMessageResponse> deleteMicroschema(String uuid);

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
