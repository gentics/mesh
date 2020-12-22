package com.gentics.mesh.rest.client.method;

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
}
