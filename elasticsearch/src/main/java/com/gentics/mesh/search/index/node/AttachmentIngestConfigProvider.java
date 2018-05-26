package com.gentics.mesh.search.index.node;

import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.schema.Schema;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * This will is used to generate the Elasticsearch ingest attachment plugin configuration.
 */
@Singleton
public class AttachmentIngestConfigProvider {

	@Inject
	public AttachmentIngestConfigProvider() {
	}

	public JsonObject getConfig(Schema schema) {

		Set<String> binaryFieldNames = schema.getFields().stream()
			.filter(f -> f.getType().equals(FieldTypes.BINARY.toString()))
			.map(f -> f.getName())
			.collect(Collectors.toSet());

		JsonObject config = new JsonObject();
		config.put("description", "Extract attachment information");
		JsonArray processors = new JsonArray();
		for (String fieldName : binaryFieldNames) {

			JsonObject settings = new JsonObject();
			settings.put("field", "fields." + fieldName + ".data");
			JsonArray props = new JsonArray();
			props.add("content");
			props.add("title");
			props.add("language");
			props.add("author");
			props.add("date");

			settings.put("properties", props);
			settings.put("target_field", "fields." + fieldName + ".content");
			settings.put("ignore_missing", true);

			JsonObject processor = new JsonObject();
			processor.put("attachment", settings);

			processors.add(processor);
		}
		config.put("processors", processors);
		return config;

	}

}
