package com.gentics.mesh.search.index.tagfamily;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.search.index.AbstractTransformator;

import io.vertx.core.json.JsonObject;


public class TagFamilyTransformator extends AbstractTransformator<TagFamily> {

	@Override
	public JsonObject toDocument(TagFamily tagFamily) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.put(NAME_KEY, tagFamily.getName());
		addBasicReferences(jsonObject, tagFamily);
		addTags(jsonObject, tagFamily.getTagRoot().findAll());
		addProject(jsonObject, tagFamily.getProject());
		return jsonObject;
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, fieldType(STRING, NOT_ANALYZED));
		return props;
	}
}
