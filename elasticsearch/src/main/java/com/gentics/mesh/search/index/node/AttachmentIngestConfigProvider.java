package com.gentics.mesh.search.index.node;

import java.util.Optional;
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

	public Optional<JsonObject> getConfig(Schema schema) {

		Set<String> binaryFieldNames = schema.getFields().stream()
			.filter(f -> f.getType().equals(FieldTypes.BINARY.toString()))
			.map(f -> f.getName())
			.collect(Collectors.toSet());

		if (binaryFieldNames.size() == 0) {
			return Optional.empty();
		} else {
			JsonObject config = new JsonObject();
			config.put("description", "Extract attachment information");
			JsonArray processors = new JsonArray();
			for (String fieldName : binaryFieldNames) {
				JsonObject processor = new JsonObject();
				processor.put("attachment", new JsonObject().put("field", "fields." + fieldName + ".data"));
				processors.add(processor);
			}
			config.put("processors", processors);
			return Optional.of(config);
		}
	}

}
