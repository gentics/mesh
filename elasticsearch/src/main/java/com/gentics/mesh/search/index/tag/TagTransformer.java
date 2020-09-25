package com.gentics.mesh.search.index.tag;

import static com.gentics.mesh.core.data.Bucket.BUCKET_ID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.search.index.AbstractTransformer;
import com.gentics.mesh.search.index.MappingHelper;
import com.gentics.mesh.util.ETag;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for tag search index documents.
 */
@Singleton
public class TagTransformer extends AbstractTransformer<HibTag> {

	@Inject
	public TagTransformer() {
	}

	public String generateVersion(HibTag tag) {
		HibProject project = tag.getProject();
		HibTagFamily tagFamily = tag.getTagFamily();

		StringBuilder builder = new StringBuilder();
		builder.append(tag.getElementVersion());
		builder.append("|");
		builder.append(project.getUuid() + project.getName());
		builder.append("|");
		builder.append(tagFamily.getElementVersion());
		// No need to add users since the creator/editor edge affects the tag version
		return ETag.hash(builder.toString());
	}

	/**
	 * Transform the tag to the document which can be stored in ES.
	 * 
	 * @param tag
	 * @param withVersion
	 *            Whether to include the version number.
	 * @return
	 */
	@Override
	public JsonObject toDocument(HibTag tag) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, tag.getName());
		addBasicReferences(document, tag);
		addPermissionInfo(document, tag);
		addTagFamily(document, tag.getTagFamily());
		addProject(document, tag.getProject());
		document.put(MappingHelper.VERSION_KEY, generateVersion(tag));
		document.put(BUCKET_ID_KEY, tag.getBucketId());
		return document;
	}

	/**
	 * Add the tag family fields to the document.
	 * 
	 * @param document
	 * @param tagFamily
	 */
	public void addTagFamily(JsonObject document, HibTagFamily tagFamily) {
		JsonObject info = new JsonObject();
		info.put(NAME_KEY, tagFamily.getName());
		info.put(UUID_KEY, tagFamily.getUuid());
		document.put("tagFamily", info);
	}

}
