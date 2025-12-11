package com.gentics.mesh.search.index.group;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.trigramTextType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.search.Compliance;
import com.gentics.mesh.search.index.AbstractMappingProvider;

import io.vertx.core.json.JsonObject;

/**
 * Elasticsearch mapping provider for group indices.
 */
@Singleton
public class GroupMappingProvider extends AbstractMappingProvider {

	@Inject
	public GroupMappingProvider(Compliance compliance) {
		super(compliance);
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, trigramTextType());
		return props;
	}
}
