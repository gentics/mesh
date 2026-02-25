package com.gentics.mesh.search.index.tagfamily;

import static com.gentics.mesh.core.data.Bucket.BUCKET_ID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.util.PreparationUtil.getPreparedData;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.handler.DataHolderContext;
import com.gentics.mesh.search.index.AbstractTransformer;
import com.gentics.mesh.search.index.MappingHelper;
import com.gentics.mesh.util.ETag;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for tagfamily search index documents.
 */
@Singleton
public class TagFamilyTransformer extends AbstractTransformer<HibTagFamily> {

	@Inject
	public TagFamilyTransformer() {
	}

	@Override
	public String generateVersion(HibTagFamily tagFamily, DataHolderContext dhc) {
		TagDao tagDao = Tx.get().tagDao();

		HibProject project = tagFamily.getProject();
		StringBuilder builder = new StringBuilder();
		builder.append(tagFamily.getElementVersion());
		builder.append("|");
		List<? extends HibTag> tags = getPreparedData(tagFamily, dhc, "tagFamily", "tags", f -> tagDao.findAll(f).list());
		tags.forEach(tag -> {
			builder.append(tag.getElementVersion());
			builder.append("|");
		});
		builder.append(project.getUuid() + project.getName());
		return ETag.hash(builder.toString());
	}

	/**
	 * Transform the role to the document which can be stored in ES.
	 * @param tagFamily
	 * 
	 * @return
	 */
	@Override
	public JsonObject toDocument(HibTagFamily tagFamily, DataHolderContext dhc) {
		TagDao tagDao = Tx.get().tagDao();

		JsonObject document = new JsonObject();
		document.put(NAME_KEY, tagFamily.getName());
		addBasicReferences(document, tagFamily);
		List<? extends HibTag> tags = getPreparedData(tagFamily, dhc, "tagFamily", "tags", f -> tagDao.findAll(f).list());
		addTags(document, tags);
		addProject(document, tagFamily.getProject());
		addPermissionInfo(document, tagFamily, "tagFamily", dhc);
		document.put(MappingHelper.VERSION_KEY, generateVersion(tagFamily, dhc));
		document.put(BUCKET_ID_KEY, tagFamily.getBucketId());
		return document;
	}

}
