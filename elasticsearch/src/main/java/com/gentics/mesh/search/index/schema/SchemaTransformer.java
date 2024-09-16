package com.gentics.mesh.search.index.schema;

import static com.gentics.mesh.core.data.Bucket.BUCKET_ID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.DESCRIPTION_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.search.index.AbstractTransformer;
import com.gentics.mesh.util.ETag;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for schemas.
 */
@Singleton
public class SchemaTransformer extends AbstractTransformer<HibSchema> {

	@Inject
	public SchemaTransformer() {
	}

	@Override
	public String generateVersion(HibSchema container) {
		StringBuilder builder = new StringBuilder();
		builder.append(container.getElementVersion());
		builder.append("|");
		builder.append(container.getLatestVersion().getElementVersion());
		// No need to add users since the creator/editor edge affects the schema version
		return ETag.hash(builder.toString());
	}

	@Override
	public JsonObject toDocument(HibSchema container) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, container.getName());
		document.put(DESCRIPTION_KEY, container.getLatestVersion().getSchema().getDescription());
		addBasicReferences(document, container);
		addPermissionInfo(document, container);
		document.put(VERSION_KEY, generateVersion(container));
		document.put(BUCKET_ID_KEY, container.getBucketId());
		return document;
	}

}
