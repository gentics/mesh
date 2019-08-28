package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.schema.MicroschemaListResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

public interface SchemaClientMethods {

	/**
	 * Create a new schema using the given request.
	 *
	 * @param request
	 * @param parameters
	 * @return
	 */
	MeshRequest<SchemaResponse> createSchema(SchemaCreateRequest request, ParameterProvider... parameters);

	/**
	 * Create a new schema using the given uuid and request.
	 *
	 * @param uuid
	 *            Uuid of the schema
	 * @param request
	 * @param parameters
	 * @return
	 */
	MeshRequest<SchemaResponse> createSchema(String uuid, SchemaCreateRequest request, ParameterProvider... parameters);

	/**
	 * Load the schema with the given uuid.
	 *
	 * @param uuid
	 *            Schema uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<SchemaResponse> findSchemaByUuid(String uuid, ParameterProvider... parameters);

	/**
	 * Update the schema with the given request.
	 *
	 * @param uuid
	 *            Schema uuid
	 * @param request
	 *            Update request
	 * @param parameters
	 * @return
	 */
	MeshRequest<GenericMessageResponse> updateSchema(String uuid, SchemaUpdateRequest request, ParameterProvider... parameters);

	/**
	 * Compare the given schema with the currently stored one and return a list of schema changes.
	 *
	 * @param uuid
	 *            Schema uuid
	 * @param request
	 * @return
	 */
	MeshRequest<SchemaChangesListModel> diffSchema(String uuid, Schema request);

	/**
	 * Delete the given schema
	 *
	 * @param uuid
	 *            Schema uuid
	 * @return
	 */
	MeshRequest<EmptyResponse> deleteSchema(String uuid);

	/**
	 * Load multiple schemas.
	 *
	 * @param parameters
	 * @return
	 */
	MeshRequest<SchemaListResponse> findSchemas(ParameterProvider... parameters);

	/**
	 * Load multiple microschemas.
	 *
	 * @param parameters
	 * @return
	 */
	MeshRequest<MicroschemaListResponse> findMicroschemas(ParameterProvider... parameters);

	/**
	 * Apply the given list of changes to the schema which is identified by the given uuid.
	 *
	 * @param uuid
	 *            Schema uuid
	 * @param changes
	 *            List of changes
	 * @return
	 */
	MeshRequest<GenericMessageResponse> applyChangesToSchema(String uuid, SchemaChangesListModel changes);

	/**
	 * Assign a schema to the project.
	 *
	 * @param projectName
	 *            project name
	 * @param schemaUuid
	 *            schema uuid
	 * @return
	 */
	MeshRequest<SchemaResponse> assignSchemaToProject(String projectName, String schemaUuid);

	/**
	 * Unassign a schema from the project
	 *
	 * @param projectName
	 *            project name
	 * @param schemaUuid
	 *            schema uuid
	 * @return
	 */
	MeshRequest<EmptyResponse> unassignSchemaFromProject(String projectName, String schemaUuid);

	/**
	 * Find all schemas assigned to the project
	 *
	 * @param projectName
	 *            project name
	 * @param parameters
	 * @return
	 */
	MeshRequest<SchemaListResponse> findSchemas(String projectName, ParameterProvider... parameters);

	/**
	 * Assign a microschema to the project
	 *
	 * @param projectName
	 *            project name
	 * @param microschemaUuid
	 *            microschema uuid
	 * @return
	 */
	MeshRequest<MicroschemaResponse> assignMicroschemaToProject(String projectName, String microschemaUuid);

	/**
	 * Unassign a microschema from the project
	 *
	 * @param projectName
	 *            project name
	 * @param microschemaUuid
	 *            microschema uuid
	 * @return
	 */
	MeshRequest<EmptyResponse> unassignMicroschemaFromProject(String projectName, String microschemaUuid);

	/**
	 * Find all microschemas assigned to the project
	 *
	 * @param projectName
	 *            project name
	 * @param parameters
	 * @return
	 */
	MeshRequest<MicroschemaListResponse> findMicroschemas(String projectName, ParameterProvider... parameters);
}
