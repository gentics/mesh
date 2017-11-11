package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.validation.SchemaValidationResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;

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
	MeshRequest<SchemaValidationResponse> validateSchema(Schema schema);

	/**
	 * Validate the microschema.
	 * 
	 * @param microschema
	 * @return
	 */
	MeshRequest<SchemaValidationResponse> validateMicroschema(Microschema microschema);
}
