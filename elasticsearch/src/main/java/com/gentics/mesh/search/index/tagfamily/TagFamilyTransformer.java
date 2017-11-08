package com.gentics.mesh.search.index.tagfamily;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.search.index.AbstractTransformer;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for tagfamily search index documents.
 */
@Singleton
public class TagFamilyTransformer extends AbstractTransformer<TagFamily> {

	@Inject
	public TagFamilyTransformer() {
	}

	@Override
	public JsonObject toDocument(TagFamily tagFamily) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, tagFamily.getName());
		addBasicReferences(document, tagFamily);
		addTags(document, tagFamily.findAllIt());
		addProject(document, tagFamily.getProject());
		addPermissionInfo(document, tagFamily);
		return document;
	}

}
