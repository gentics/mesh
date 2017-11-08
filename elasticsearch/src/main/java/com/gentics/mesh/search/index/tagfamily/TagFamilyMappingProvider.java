package com.gentics.mesh.search.index.tagfamily;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.OBJECT;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.notAnalyzedType;
import static com.gentics.mesh.search.index.MappingHelper.trigramStringType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.search.index.AbstractMappingProvider;

import io.vertx.core.json.JsonObject;

@Singleton
public class TagFamilyMappingProvider extends AbstractMappingProvider {

	@Inject
	public TagFamilyMappingProvider() {
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, trigramStringType());

		// TODO tags

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
