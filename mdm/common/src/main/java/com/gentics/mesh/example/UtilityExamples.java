package com.gentics.mesh.example;

import java.io.IOException;

import org.apache.commons.io.IOUtils;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.validation.SchemaValidationResponse;
import com.gentics.mesh.core.rest.validation.ValidationStatus;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class UtilityExamples extends AbstractExamples {

	private static final Logger log = LoggerFactory.getLogger(UtilityExamples.class);

	public SchemaValidationResponse createValidationResponse() {
		JsonObject esConfig = new JsonObject();
		try {
			esConfig = new JsonObject(IOUtils.toString(getClass().getResourceAsStream("/json/exampleMapping.json")));
		} catch (IOException e) {
			log.error("Could not load example mapping.", e);
		}
		return new SchemaValidationResponse().setStatus(ValidationStatus.VALID)
				.setMessage(new GenericMessageResponse("Schema validation was successful.")).setElasticsearch(esConfig);
	}

}
