package com.gentics.mesh.search.index.microschema;

import static com.gentics.mesh.search.index.MappingHelper.DESCRIPTION_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.trigramStringType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.search.index.AbstractTransformator;

import io.vertx.core.json.JsonObject;

/**
 * Transformator for microschema search index documents.
 */
@Singleton
public class MicroschemaTransformator extends AbstractTransformator<MicroschemaContainer> {

	@Inject
	public MicroschemaTransformator() {
	}

	@Override
	public JsonObject toDocument(MicroschemaContainer microschema) {
		JsonObject info = new JsonObject();
		addBasicReferences(info, microschema);
		info.put(NAME_KEY, microschema.getName());
		// map.put(DESCRIPTION_KEY, microschema.getSchema().getDescription());
		return info;
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, trigramStringType());
		props.put(DESCRIPTION_KEY, trigramStringType());
		return props;
	}
}
