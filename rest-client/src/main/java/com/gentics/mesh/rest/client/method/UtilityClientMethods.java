package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.NameOrUUIDsRequest;
import com.gentics.mesh.core.rest.schema.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.validation.SchemaValidationResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

/**
 * REST client methods for utility operations.
 */
public interface UtilityClientMethods {

	/**
	 * Resolve links in the given string.
	 * 
	 * @param body
	 *            request body
	 * @param parameters
	 * @return
	 */
	MeshRequest<String> resolveLinks(String body, ParameterProvider... parameters);

	/**
	 * Validate the schema.
	 * 
	 * @param schema
	 * @return
	 */
	MeshRequest<SchemaValidationResponse> validateSchema(SchemaModel schema);

	/**
	 * Validate the microschema.
	 * 
	 * @param microschemaModel
	 * @return
	 */
	MeshRequest<SchemaValidationResponse> validateMicroschema(MicroschemaModel microschemaModel);


	/**
	 * Request a purge for unused/empty schema versions.
	 * @return status message response
	 */
	default MeshRequest<GenericMessageResponse> purgeSchemaVersions() {
		return purgeSchemaVersions(null);
	}

	/**
	 * Request a purge for unused/empty schema versions.
	 * @param request optional limits
	 * @return status message response
	 */
	MeshRequest<GenericMessageResponse> purgeSchemaVersions(NameOrUUIDsRequest request);

	/**
	 * Request a purge for unused/empty microschema versions.
	 * @return status message response
	 */
	default MeshRequest<GenericMessageResponse> purgeMicroschemaVersions() {
		return purgeMicroschemaVersions(null);
	}

	/**
	 * Request a purge for unused/empty microschema versions.
	 * @param request optional limits
	 * @return status message response
	 */
	MeshRequest<GenericMessageResponse> purgeMicroschemaVersions(NameOrUUIDsRequest request);
}
