package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaMigrationResponse;
import com.gentics.mesh.query.QueryParameterProvider;

import io.vertx.core.Future;

public interface MicroschemaClientMethods {
	/**
	 * Create a new microschema using the given request.
	 * 
	 * @param request
	 *            create request
	 * @return future for the microschema response
	 */
	Future<Microschema> createMicroschema(Microschema request);

	/**
	 * Load the microschema with the given UUID.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	Future<Microschema> findMicroschemaByUuid(String uuid, QueryParameterProvider... parameters);

	/**
	 * Update the microschema with the given request.
	 * 
	 * @param uuid
	 *            Microschema UUID
	 * @param request
	 *            Update request
	 * @return
	 */
	Future<Microschema> updateMicroschema(String uuid, Microschema request);

	/**
	 * Delete the given microschema.
	 * 
	 * @param uuid
	 *            Microschema UUID
	 * @return
	 */
	Future<GenericMessageResponse> deleteMicroschema(String uuid);

	/**
	 * Apply the given set of changes to the microschema.
	 * 
	 * @param uuid
	 *            Microschema UUID
	 * @param changes
	 * @return
	 */
	Future<SchemaMigrationResponse> applyChangesToMicroschema(String uuid, SchemaChangesListModel changes);

	/**
	 * Compare the given microschema with a currently stored one and return a list of changes.
	 * 
	 * @param uuid
	 * @param request
	 * @return
	 */
	Future<SchemaChangesListModel> diffMicroschema(String uuid, Microschema request);
}
