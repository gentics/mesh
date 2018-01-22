package com.gentics.mesh.search.index.user;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.OBJECT;
import static com.gentics.mesh.search.index.MappingHelper.TEXT;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.notAnalyzedType;
import static com.gentics.mesh.search.index.MappingHelper.trigramTextType;

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
		props.put(UserTransformer.USERNAME_KEY, trigramTextType());
		props.put(UserTransformer.LASTNAME_KEY, trigramTextType());
		props.put(UserTransformer.FIRSTNAME_KEY, trigramTextType());
		props.put(UserTransformer.EMAIL_KEY, notAnalyzedType(TEXT));
		props.put(UserTransformer.NODEREFERECE_KEY, notAnalyzedType(TEXT));
		props.put(UserTransformer.GROUPS_KEY, new JsonObject().put("type", OBJECT).put("properties", new JsonObject().put(NAME_KEY,
				trigramTextType()).put(UUID_KEY, notAnalyzedType(TEXT))));

		return props;
	}
}
