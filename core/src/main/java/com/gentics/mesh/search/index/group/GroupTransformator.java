package com.gentics.mesh.search.index.group;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.search.index.AbstractTransformator;

import io.vertx.core.json.JsonObject;

/**
 * Transformator for group search index documents.
 */
@Singleton
public class GroupTransformator extends AbstractTransformator<Group> {

	@Inject
	public GroupTransformator() {
	}

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
