package com.gentics.mesh.search.index.project;

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
		StringBuilder builder = new StringBuilder();
		builder.append(project.getElementVersion());
		// No need to add users since the creator/editor edge affects the project version
		return ETag.hash(builder.toString());
	}

	private JsonObject toDocument(Project project, boolean withVersion) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, project.getName());
		addBasicReferences(document, project);
		addPermissionInfo(document, project);
		if (withVersion) {
			document.put(VERSION_KEY, generateVersion(project));
		}
		return document;
	}

	@Override
	public JsonObject toDocument(Project project) {
		return toDocument(project, true);
	}

}
