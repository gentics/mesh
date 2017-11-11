package com.gentics.mesh.search.index.schema;

import static com.gentics.mesh.search.index.MappingHelper.DESCRIPTION_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.trigramStringType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.search.index.AbstractMappingProvider;

import io.vertx.core.json.JsonObject;

@Singleton
public class SchemaMappingProvider extends AbstractMappingProvider {

	@Inject
	public SchemaMappingProvider() {
	}
	
	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, trigramStringType());
		props.put(DESCRIPTION_KEY, trigramStringType());
		return props;
	}
}
