package com.gentics.mesh.search.index.tagfamily;

import static com.gentics.mesh.search.index.Bucket.BUCKET_ID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Project;
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
		Project project = tagFamily.getProject();

		StringBuilder builder = new StringBuilder();
		builder.append(tagFamily.getElementVersion());
		builder.append("|");
		tagFamily.findAll().forEach(tag -> {
			builder.append(tag.getElementVersion());
			builder.append("|");
		});
		builder.append(project.getUuid() + project.getName());
		return ETag.hash(builder.toString());
	}

	/**
	 * Transform the role to the document which can be stored in ES.
	 * 
	 * @param tagFamily
	 * @return
	 */
	@Override
	public JsonObject toDocument(TagFamily tagFamily) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, tagFamily.getName());
		addBasicReferences(document, tagFamily);
		addTags(document, tagFamily.findAll());
		addProject(document, tagFamily.getProject());
		addPermissionInfo(document, tagFamily);
		document.put(MappingHelper.VERSION_KEY, generateVersion(tagFamily));
		document.put(BUCKET_ID_KEY, tagFamily.getBucketId());
		return document;
	}

}
