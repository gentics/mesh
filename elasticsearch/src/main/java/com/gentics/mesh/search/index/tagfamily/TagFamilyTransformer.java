package com.gentics.mesh.search.index.tagfamily;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.search.index.AbstractTransformer;
import com.gentics.mesh.search.index.MappingHelper;
import com.gentics.mesh.util.ETag;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for tagfamily search index documents.
 */
@Singleton
public class TagFamilyTransformer extends AbstractTransformer<TagFamily> {

	@Inject
	public TagFamilyTransformer() {
	}

	public String generateVersion(TagFamily tagFamily) {
		return ETag.hash(toDocument(tagFamily, false).encode());
	}

	/**
	 * Transform the role to the document which can be stored in ES.
	 * 
	 * @param tagFamily
	 * @param withVersion
	 *            Whether to include the version number.
	 * @return
	 */
	private JsonObject toDocument(TagFamily tagFamily, boolean withVersion) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, tagFamily.getName());
		addBasicReferences(document, tagFamily);
		addTags(document, tagFamily.findAllIt());
		addProject(document, tagFamily.getProject());
		addPermissionInfo(document, tagFamily);
		if (withVersion) {
			document.put(MappingHelper.VERSION_KEY, generateVersion(tagFamily));
		}
		return document;
	}

	@Override
	public JsonObject toDocument(TagFamily tagFamily) {
		return toDocument(tagFamily, true);
	}

}
