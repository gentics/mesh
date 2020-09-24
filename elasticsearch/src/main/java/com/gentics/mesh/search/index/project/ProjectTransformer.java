package com.gentics.mesh.search.index.project;

import static com.gentics.mesh.search.index.Bucket.BUCKET_ID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.search.index.AbstractTransformer;
import com.gentics.mesh.util.ETag;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for project search index documents.
 */
@Singleton
public class ProjectTransformer extends AbstractTransformer<Project> {

	@Inject
	public ProjectTransformer() {
	}

	public String generateVersion(Project project) {
		// No need to add users since the creator/editor edge affects the project version
		return ETag.hash(project.getElementVersion());
	}

	@Override
	public JsonObject toDocument(Project project) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, project.getName());
		addBasicReferences(document, project);
		addPermissionInfo(document, project);
		document.put(VERSION_KEY, generateVersion(project));
		document.put(BUCKET_ID_KEY, project.getBucketId());
		return document;
	}

}
