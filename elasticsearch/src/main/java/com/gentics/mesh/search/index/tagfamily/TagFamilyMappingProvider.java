package com.gentics.mesh.search.index.tagfamily;

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

/**
 * Class which will generate the needed mapping for the tag family index.
 */
@Singleton
public class TagFamilyMappingProvider extends AbstractMappingProvider {

	@Inject
	public TagFamilyMappingProvider(MeshOptions options) {
		super(options);
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, trigramTextType());

		// TODO tags

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
