package com.gentics.mesh.search.index.schema;

import static com.gentics.mesh.search.index.MappingHelper.DESCRIPTION_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.search.index.AbstractTransformer;
import com.gentics.mesh.util.ETag;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for schemas.
 */
@Singleton
public class SchemaTransformer extends AbstractTransformer<SchemaContainer> {

	@Inject
	public SchemaTransformer() {
	}

	public String generateVersion(SchemaContainer container) {
		StringBuilder builder = new StringBuilder();
		builder.append(container.getElementVersion());
		builder.append("|");
		builder.append(container.getLatestVersion().getElementVersion());
		// No need to add users since the creator/editor edge affects the schema version
		return ETag.hash(builder.toString());
	}

	private JsonObject toDocument(SchemaContainer container, boolean withVersion) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, container.getName());
		document.put(DESCRIPTION_KEY, container.getLatestVersion().getSchema().getDescription());
		addBasicReferences(document, container);
		addPermissionInfo(document, container);
		if (withVersion) {
			document.put(VERSION_KEY, generateVersion(container));
		}
		return document;
	}

	@Override
	public JsonObject toDocument(SchemaContainer container) {
		return toDocument(container, true);
	}

}
