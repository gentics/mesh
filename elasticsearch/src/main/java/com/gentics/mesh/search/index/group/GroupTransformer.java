package com.gentics.mesh.search.index.group;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.search.index.AbstractTransformer;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for group search index documents.
 */
@Singleton
public class GroupTransformer extends AbstractTransformer<Group> {

	@Inject
	public GroupTransformer() {
	}

	@Override
	public JsonObject toDocument(Group group) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, group.getName());
		addBasicReferences(document, group);
		addPermissionInfo(document, group);
		return document;
	}

}
