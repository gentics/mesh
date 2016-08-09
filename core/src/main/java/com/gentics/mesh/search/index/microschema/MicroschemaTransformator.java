package com.gentics.mesh.search.index.microschema;

import static com.gentics.mesh.search.index.MappingHelper.ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.DESCRIPTION_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.search.index.AbstractTransformator;

import io.vertx.core.json.JsonObject;

@Component
public class MicroschemaTransformator extends AbstractTransformator<MicroschemaContainer> {

	@Override
	public JsonObject toDocument(MicroschemaContainer microschema) {
		JsonObject info = new JsonObject();
		addBasicReferences(info, microschema);
		info.put(NAME_KEY, microschema.getName());
		//map.put(DESCRIPTION_KEY, microschema.getSchema().getDescription());
		return info;
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, fieldType(STRING, NOT_ANALYZED));
		props.put(DESCRIPTION_KEY, fieldType(STRING, ANALYZED));
		return props;
	}
}
