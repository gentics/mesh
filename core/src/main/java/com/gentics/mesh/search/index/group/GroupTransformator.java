package com.gentics.mesh.search.index.group;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.search.index.AbstractTransformator;

import io.vertx.core.json.JsonObject;

public class GroupTransformator extends AbstractTransformator<Group> {

	@Override
	public JsonObject toDocument(Group group) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, group.getName());
		addBasicReferences(document, group);
		return document;
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, fieldType(STRING, NOT_ANALYZED));
		return props;
	}

}
