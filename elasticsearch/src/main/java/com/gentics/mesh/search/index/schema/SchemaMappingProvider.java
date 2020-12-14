package com.gentics.mesh.search.index.schema;

import static com.gentics.mesh.search.index.MappingHelper.DESCRIPTION_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.trigramTextType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.index.AbstractMappingProvider;

import io.vertx.core.json.JsonObject;

/**
 * ES Mapping provider for schema indices.
 */
@Singleton
public class SchemaMappingProvider extends AbstractMappingProvider {

	@Inject
	public SchemaMappingProvider(MeshOptions options) {
		super(options);
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, trigramTextType());
		props.put(DESCRIPTION_KEY, trigramTextType());
		return props;
	}
}
