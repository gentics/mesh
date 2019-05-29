package com.gentics.mesh.search.index;

import static com.gentics.mesh.search.SearchProvider.DEFAULT_TYPE;
import static com.gentics.mesh.search.index.MappingHelper.DATE;
import static com.gentics.mesh.search.index.MappingHelper.KEYWORD;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.VERSION_KEY;
import static com.gentics.mesh.search.index.MappingHelper.notAnalyzedType;

import io.vertx.core.json.JsonObject;

public abstract class AbstractMappingProvider implements MappingProvider {

	public static final String ROLE_UUIDS = "_roleUuids";

	@Override
	public JsonObject getMapping() {
		JsonObject mapping = new JsonObject();

		// Enhance mappings with generic/common field types
		JsonObject mappingProperties = getMappingProperties();
		mappingProperties.put(UUID_KEY, notAnalyzedType(KEYWORD));
		mappingProperties.put(VERSION_KEY, notAnalyzedType(KEYWORD));
		mappingProperties.put("created", notAnalyzedType(DATE));
		mappingProperties.put("edited", notAnalyzedType(DATE));
		mappingProperties.put("editor", getUserReferenceMapping());
		mappingProperties.put("creator", getUserReferenceMapping());
		mappingProperties.put(ROLE_UUIDS, notAnalyzedType(KEYWORD));

		JsonObject typeMapping = new JsonObject();
		typeMapping.put("properties", mappingProperties);

		// All mappings must be strict. We don't allow automatic type detection 
		// since this may cause problems if the type changes in between documents.
		typeMapping.put("date_detection", false);
		typeMapping.put("numeric_detection", false);

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
		userProps.put("uuid", notAnalyzedType(KEYWORD));
		mapping.put("properties", userProps);
		return mapping;
	}
}
