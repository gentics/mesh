package com.gentics.mesh.search.index.tag;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.search.index.AbstractTransformer;
import com.gentics.mesh.search.index.MappingHelper;
import com.gentics.mesh.util.ETag;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for tag search index documents.
 */
@Singleton
public class TagTransformer extends AbstractTransformer<Tag> {

	@Inject
	public TagTransformer() {
	}

	public String generateVersion(Tag tag) {
		return ETag.hash(toDocument(tag, false).encode());
	}

	/**
	 * Transform the tag to the document which can be stored in ES.
	 * 
	 * @param tag
	 * @param withVersion
	 *            Whether to include the version number.
	 * @return
	 */
	private JsonObject toDocument(Tag tag, boolean withVersion) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, tag.getName());
		addBasicReferences(document, tag);
		addPermissionInfo(document, tag);
		addTagFamily(document, tag.getTagFamily());
		addProject(document, tag.getProject());
		if (withVersion) {
			document.put(MappingHelper.VERSION_KEY, generateVersion(tag));
		}
		return document;
	}

	@Override
	public JsonObject toDocument(Tag tag) {
		return toDocument(tag, true);
	}

	/**
	 * Add the tag family fields to the document.
	 * 
	 * @param document
	 * @param tagFamily
	 */
	public void addTagFamily(JsonObject document, TagFamily tagFamily) {
		JsonObject info = new JsonObject();
		info.put(NAME_KEY, tagFamily.getName());
		info.put(UUID_KEY, tagFamily.getUuid());
		document.put("tagFamily", info);
	}

}
