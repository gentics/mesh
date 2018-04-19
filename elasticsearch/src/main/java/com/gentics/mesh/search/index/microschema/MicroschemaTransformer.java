package com.gentics.mesh.search.index.microschema;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.search.index.AbstractTransformer;
import com.gentics.mesh.util.ETag;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for microschema search index documents.
 */
@Singleton
public class MicroschemaTransformer extends AbstractTransformer<MicroschemaContainer> {

	@Inject
	public MicroschemaTransformer() {
	}

	public String generateVersion(MicroschemaContainer microschema) {
		// No need to add users since the creator/editor edge affects the microschema version
		return ETag.hash(microschema.getElementVersion());
	}

	private JsonObject toDocument(MicroschemaContainer microschema, boolean withVersion) {
		JsonObject document = new JsonObject();
		addBasicReferences(document, microschema);
		document.put(NAME_KEY, microschema.getName());
		addPermissionInfo(document, microschema);
		// map.put(DESCRIPTION_KEY, microschema.getSchema().getDescription());
		if (withVersion) {
			document.put(VERSION_KEY, generateVersion(microschema));
		}
		return document;
	}

	@Override
	public JsonObject toDocument(MicroschemaContainer microschema) {
		return toDocument(microschema, true);
	}

}
