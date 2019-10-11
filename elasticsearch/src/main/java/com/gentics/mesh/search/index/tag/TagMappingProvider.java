package com.gentics.mesh.search.index.tag;

import static com.gentics.mesh.search.index.MappingHelper.KEYWORD;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.OBJECT;
import static com.gentics.mesh.search.index.MappingHelper.notAnalyzedType;
import static com.gentics.mesh.search.index.MappingHelper.trigramTextType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.index.AbstractMappingProvider;

import io.vertx.core.json.JsonObject;

@Singleton
public class TagMappingProvider extends AbstractMappingProvider {

	@Inject
	public TagMappingProvider(MeshOptions options) {
		super(options);
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, trigramTextType());

		// tagFamily
		JsonObject tagFamilyMapping = new JsonObject();
		tagFamilyMapping.put("type", OBJECT);
		JsonObject schemaMappingProperties = new JsonObject();
		schemaMappingProperties.put("uuid", notAnalyzedType(KEYWORD));
		schemaMappingProperties.put("name", trigramTextType());
		tagFamilyMapping.put("properties", schemaMappingProperties);
		props.put("tagFamily", tagFamilyMapping);

		// project
		JsonObject projectMapping = new JsonObject();
		projectMapping.put("type", OBJECT);
		JsonObject projectMappingProps = new JsonObject();
		projectMappingProps.put("name", trigramTextType());
		projectMappingProps.put("uuid", notAnalyzedType(KEYWORD));
		projectMapping.put("properties", projectMappingProps);
		props.put("project", projectMapping);

		return props;
	}

}
