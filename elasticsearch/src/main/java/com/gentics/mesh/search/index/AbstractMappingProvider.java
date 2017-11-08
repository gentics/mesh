package com.gentics.mesh.search.index;

import static com.gentics.mesh.search.index.MappingHelper.DATE;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.notAnalyzedType;

import io.vertx.core.json.JsonObject;

public abstract class AbstractMappingProvider implements MappingProvider {

	@Override
	public JsonObject getMapping(String type) {
		JsonObject mapping = new JsonObject();

		// Enhance mappings with generic/common field types
		JsonObject mappingProperties = getMappingProperties();
		mappingProperties.put(UUID_KEY, notAnalyzedType(STRING));
		mappingProperties.put("created", notAnalyzedType(DATE));
		mappingProperties.put("edited", notAnalyzedType(DATE));
		mappingProperties.put("editor", getUserReferenceMapping());
		mappingProperties.put("creator", getUserReferenceMapping());
		mappingProperties.put("_roleUuids", notAnalyzedType(STRING));

		JsonObject typeMapping = new JsonObject();
		typeMapping.put("properties", mappingProperties);

		mapping.put(type, typeMapping);
		return mapping;
	}

	/**
	 * Return the user reference mapping.
	 * 
	 * @return
	 */
	private JsonObject getUserReferenceMapping() {
		JsonObject mapping = new JsonObject();
		mapping.put("type", "object");
		JsonObject userProps = new JsonObject();
		userProps.put("uuid", notAnalyzedType(STRING));
		mapping.put("properties", userProps);
		return mapping;
	}
}
