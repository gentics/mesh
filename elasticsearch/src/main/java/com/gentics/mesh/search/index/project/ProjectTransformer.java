package com.gentics.mesh.search.index.project;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.search.index.AbstractTransformer;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for project search index documents.
 */
@Singleton
public class ProjectTransformer extends AbstractTransformer<Project> {

	@Inject
	public ProjectTransformer() {
	}

	@Override
	public JsonObject toDocument(Project project) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, project.getName());
		addBasicReferences(document, project);
		addPermissionInfo(document, project);
		return document;
	}

}
