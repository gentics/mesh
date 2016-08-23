package com.gentics.mesh.search.index.tag;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.search.index.AbstractTransformator;

import io.vertx.core.json.JsonObject;

public class TagTransformator extends AbstractTransformator<Tag> {

	@Override
	public JsonObject toDocument(Tag tag) {
		JsonObject document = new JsonObject();
		Map<String, String> tagFields = new HashMap<>();
		tagFields.put(NAME_KEY, tag.getName());
		document.put("fields", tagFields);
		addBasicReferences(document, tag);
		addTagFamily(document, tag.getTagFamily());
		addProject(document, tag.getProject());
		return document;
	}

	public void addTagFamily(JsonObject document, TagFamily tagFamily) {
		JsonObject info = new JsonObject();
		info.put(NAME_KEY, tagFamily.getName());
		info.put(UUID_KEY, tagFamily.getUuid());
		document.put("tagFamily", info);
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, fieldType(STRING, NOT_ANALYZED));
		return props;
	}
	
}
