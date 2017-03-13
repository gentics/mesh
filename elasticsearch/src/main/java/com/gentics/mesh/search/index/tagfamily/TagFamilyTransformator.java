package com.gentics.mesh.search.index.tagfamily;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.OBJECT;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.notAnalyzedType;
import static com.gentics.mesh.search.index.MappingHelper.trigramStringType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.search.index.AbstractTransformator;

import io.vertx.core.json.JsonObject;

/**
 * Transformator for tagfamily search index documents.
 */
@Singleton
public class TagFamilyTransformator extends AbstractTransformator<TagFamily> {

	@Inject
	public TagFamilyTransformator() {
	}

	@Override
	public JsonObject toDocument(TagFamily tagFamily) {
		JsonObject jsonObject = new JsonObject();
		jsonObject.put(NAME_KEY, tagFamily.getName());
		addBasicReferences(jsonObject, tagFamily);
		addTags(jsonObject, tagFamily.findAll());
		addProject(jsonObject, tagFamily.getProject());
		return jsonObject;
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, trigramStringType());

		//TODO tags

		// project
		JsonObject projectMapping = new JsonObject();
		projectMapping.put("type", OBJECT);
		JsonObject projectMappingProps = new JsonObject();
		projectMappingProps.put("name", trigramStringType());
		projectMappingProps.put("uuid", notAnalyzedType(STRING));
		projectMapping.put("properties", projectMappingProps);
		props.put("project", projectMapping);

		return props;
	}
}
