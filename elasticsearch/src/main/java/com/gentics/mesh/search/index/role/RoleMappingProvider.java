package com.gentics.mesh.search.index.role;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.trigramTextType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.search.index.AbstractMappingProvider;

import io.vertx.core.json.JsonObject;

@Singleton
public class RoleMappingProvider extends AbstractMappingProvider {

	@Inject
	public RoleMappingProvider() {
	}

	/**
	 * Return the type specific mapping properties.
	 */
	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(NAME_KEY, trigramTextType());
		return props;
	}

}
