package com.gentics.mesh.search.index.project;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.trigramStringType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.search.index.AbstractTransformator;

import io.vertx.core.json.JsonObject;

/**
 * Transformator for project search index documents.
 */
@Singleton
public class ProjectTransformator extends AbstractTransformator<Project> {

	@Inject
	public ProjectTransformator() {
	}

	@Override
	public JsonObject toDocument(Project project) {
		JsonObject document = new JsonObject();
		document.put(NAME_KEY, project.getName());
		addBasicReferences(document, project);
		return document;
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, trigramStringType());
		return props;
	}
}
