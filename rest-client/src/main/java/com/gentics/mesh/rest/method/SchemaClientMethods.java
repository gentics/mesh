package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.QueryParameterProvider;
import com.gentics.mesh.core.rest.schema.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.SchemaResponse;
import com.gentics.mesh.core.rest.schema.SchemaUpdateRequest;
import com.gentics.mesh.rest.ClientSchemaStorage;
import com.gentics.mesh.rest.MeshRestClient;

public interface SchemaClientMethods {

	/**
	 * Create a new schema using the given request.
	 * 
	 * @param request
	 * @return
	 */
	Future<SchemaResponse> createSchema(SchemaCreateRequest request);

	/**
	 * Load the schema with the given uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	Future<SchemaResponse> findSchemaByUuid(String uuid);

	/**
	 * Update the schema with the given request.
	 * 
	 * @param uuid
	 *            Schema uuid
	 * @param request
	 *            Update request
	 * @return
	 */
	Future<SchemaResponse> updateSchema(String uuid, SchemaUpdateRequest request);

	/**
	 * Delete the given schema
	 * 
	 * @param uuid
	 *            Schema uuid
	 * @return
	 */
	Future<GenericMessageResponse> deleteSchema(String uuid);

	/**
	 * Add the given schema to the given project.
	 * 
	 * @param schemaUuid
	 * @param projectUuid
	 * @return
	 */
	Future<SchemaResponse> addSchemaToProject(String schemaUuid, String projectUuid);

	/**
	 * Remove the given schema from the given project.
	 * 
	 * @param schemaUuid
	 * @param projectUuid
	 * @return
	 */
	Future<SchemaResponse> removeSchemaFromProject(String schemaUuid, String projectUuid);

	/**
	 * Load multiple schemas.
	 * 
	 * @param parameters
	 * @return
	 */
	Future<SchemaListResponse> findSchemas(QueryParameterProvider... parameters);

	/**
	 * Initialize the schema storage. This will effectively load all schemas into the {@link ClientSchemaStorage} of the {@link MeshRestClient} that is
	 * currently in use.
	 * 
	 * @return
	 */
	Future<Void> initSchemaStorage();

}
