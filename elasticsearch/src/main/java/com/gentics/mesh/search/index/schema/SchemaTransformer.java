package com.gentics.mesh.search.index.schema;

import static com.gentics.mesh.search.index.MappingHelper.DESCRIPTION_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.search.index.AbstractTransformer;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for schemas.
 */
@Singleton
public class SchemaTransformer extends AbstractTransformer<SchemaContainer> {

	@Inject
	public SchemaTransformer() {
	}

	@Override
	public JsonObject toDocument(SchemaContainer container) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, container.getName());
		document.put(DESCRIPTION_KEY, container.getLatestVersion().getSchema().getDescription());
		addBasicReferences(document, container);
		addPermissionInfo(document, container);
		return document;
	}

	

}
