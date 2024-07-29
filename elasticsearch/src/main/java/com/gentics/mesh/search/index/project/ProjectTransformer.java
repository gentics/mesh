package com.gentics.mesh.search.index.project;

import static com.gentics.mesh.core.data.Bucket.BUCKET_ID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.search.index.AbstractTransformer;
import com.gentics.mesh.util.ETag;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for project search index documents.
 */
@Singleton
public class ProjectTransformer extends AbstractTransformer<HibProject> {

	@Inject
	public ProjectTransformer() {
	}

	@Override
	public String generateVersion(HibProject project) {
		// No need to add users since the creator/editor edge affects the project version
		return ETag.hash(project.getElementVersion());
	}

	@Override
	public JsonObject toDocument(HibProject project) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, project.getName());
		addBasicReferences(document, project);
		addPermissionInfo(document, project);
		document.put(VERSION_KEY, generateVersion(project));
		document.put(BUCKET_ID_KEY, project.getBucketId());
		return document;
	}

}
