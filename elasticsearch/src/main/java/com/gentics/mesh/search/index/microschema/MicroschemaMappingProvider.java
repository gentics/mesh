package com.gentics.mesh.search.index.microschema;

import static com.gentics.mesh.search.index.MappingHelper.DESCRIPTION_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.trigramTextType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.search.index.AbstractMappingProvider;

import io.vertx.core.json.JsonObject;

/**
 * Search index mapping provider for microschema indices.
 */
@Singleton
public class MicroschemaMappingProvider extends AbstractMappingProvider {

	@Inject
	public MicroschemaMappingProvider(AbstractMeshOptions options) {
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
