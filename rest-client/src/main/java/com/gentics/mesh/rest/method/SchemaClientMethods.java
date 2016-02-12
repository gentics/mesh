package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.query.QueryParameterProvider;
import com.gentics.mesh.rest.ClientSchemaStorage;
import com.gentics.mesh.rest.MeshRestClient;

import io.vertx.core.Future;

public interface SchemaClientMethods {

	/**
	 * Create a new schema using the given request.
	 * 
	 * @param request
	 * @return
	 */
	Future<Schema> createSchema(Schema request);

	/**
	 * Load the schema with the given uuid.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	Future<Schema> findSchemaByUuid(String uuid, QueryParameterProvider... parameters);

	/**
	 * Update the schema with the given request.
	 * 
	 * @param uuid
	 *            Schema uuid
	 * @param request
	 *            Update request
	 * @return
	 */
	Future<Schema> updateSchema(String uuid, Schema request);

	/**
	 * Compare the given schema with the currently stored one and return a list of schema changes.
	 * 
	 * @param uuid
	 * @return
	 */
	Future<SchemaChangesListModel> diffSchema(String uuid, Schema request);

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
	Future<Schema> addSchemaToProject(String schemaUuid, String projectUuid);

	/**
	 * Remove the given schema from the given project.
	 * 
	 * @param schemaUuid
	 * @param projectUuid
	 * @return
	 */
	Future<Schema> removeSchemaFromProject(String schemaUuid, String projectUuid);

	/**
	 * Load multiple schemas.
	 * 
	 * @param parameters
	 * @return
	 */
	Future<SchemaListResponse> findSchemas(QueryParameterProvider... parameters);

	/**
	 * Load multiple schemas that were assigned to the given project.
	 * 
	 * @param projectName
	 * @param parameters
	 * @return
	 */
	Future<SchemaListResponse> findSchemas(String projectName, QueryParameterProvider... parameters);

	/**
	 * Load multiple microschemas.
	 * 
	 * @param parameters
	 * @return
	 */
	Future<MicroschemaListResponse> findMicroschemas(QueryParameterProvider... parameters);

	/**
	 * Initialize the schema storage. This will effectively load all schemas into the {@link ClientSchemaStorage} of the {@link MeshRestClient} that is
	 * currently in use.
	 * 
	 * @return
	 */
	Future<Void> initSchemaStorage();

	/**
	 * Apply the given list of changes to the schema which is identified by the given uuid.
	 * 
	 * @param uuid
	 *            Schema uuid
	 * @param change
	 *            List of changes
	 * @return
	 */
	Future<GenericMessageResponse> applyChangesToSchema(String uuid, SchemaChangesListModel changes);

}
