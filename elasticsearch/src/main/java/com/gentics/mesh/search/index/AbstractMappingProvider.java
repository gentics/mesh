package com.gentics.mesh.search.index;

import static com.gentics.mesh.search.SearchProvider.DEFAULT_TYPE;
import static com.gentics.mesh.search.index.MappingHelper.DATE;
import static com.gentics.mesh.search.index.MappingHelper.TEXT;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.notAnalyzedType;

import io.vertx.core.json.JsonObject;

public abstract class AbstractMappingProvider implements MappingProvider {

	@Override
	public JsonObject getMapping() {
		JsonObject mapping = new JsonObject();

		// Enhance mappings with generic/common field types
		JsonObject mappingProperties = getMappingProperties();
		mappingProperties.put(UUID_KEY, notAnalyzedType(TEXT));
		mappingProperties.put("created", notAnalyzedType(DATE));
		mappingProperties.put("edited", notAnalyzedType(DATE));
		mappingProperties.put("editor", getUserReferenceMapping());
		mappingProperties.put("creator", getUserReferenceMapping());
		mappingProperties.put("_roleUuids", notAnalyzedType(TEXT));

		JsonObject typeMapping = new JsonObject();
		typeMapping.put("properties", mappingProperties);

		mapping.put(DEFAULT_TYPE, typeMapping);
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
		userProps.put("uuid", notAnalyzedType(TEXT));
		mapping.put("properties", userProps);
		return mapping;
	}
}
