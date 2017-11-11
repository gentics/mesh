package com.gentics.mesh.search.index.user;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.OBJECT;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.notAnalyzedType;
import static com.gentics.mesh.search.index.MappingHelper.trigramStringType;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.search.index.AbstractMappingProvider;

import io.vertx.core.json.JsonObject;

@Singleton
public class UserMappingProvider extends AbstractMappingProvider {

	@Inject
	public UserMappingProvider() {
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(UserTransformer.USERNAME_KEY, trigramStringType());
		props.put(UserTransformer.LASTNAME_KEY, trigramStringType());
		props.put(UserTransformer.FIRSTNAME_KEY, trigramStringType());
		props.put(UserTransformer.EMAIL_KEY, notAnalyzedType(STRING));
		props.put(UserTransformer.NODEREFERECE_KEY, notAnalyzedType(STRING));
		props.put(UserTransformer.GROUPS_KEY, new JsonObject().put("type", OBJECT).put("properties", new JsonObject().put(NAME_KEY,
				trigramStringType()).put(UUID_KEY, notAnalyzedType(STRING))));

		return props;
	}
}
