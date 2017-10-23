package com.gentics.mesh.search.index.user;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.OBJECT;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.notAnalyzedType;
import static com.gentics.mesh.search.index.MappingHelper.trigramStringType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.search.index.AbstractTransformer;

import io.vertx.core.json.JsonObject;

/**
 * Transformer for user search index documents.
 */
@Singleton
public class UserTransformer extends AbstractTransformer<User> {

	public static final String EMAIL_KEY = "emailaddress";
	public static final String USERNAME_KEY = "username";
	public static final String FIRSTNAME_KEY = "firstname";
	public static final String LASTNAME_KEY = "lastname";
	public static final String NODEREFERECE_KEY = "nodeReference";
	public static final String GROUPS_KEY = "groups";

	@Inject
	public UserTransformer() {
	}

	@Override
	public JsonObject toDocument(User user) {
		JsonObject document = new JsonObject();
		addBasicReferences(document, user);
		document.put(USERNAME_KEY, user.getUsername());
		document.put(EMAIL_KEY, user.getEmailAddress());
		document.put(FIRSTNAME_KEY, user.getFirstname());
		document.put(LASTNAME_KEY, user.getLastname());
		addGroups(document, user.getGroups());
		addPermissionInfo(document, user);
		Node referencedNode = user.getReferencedNode();
		if (referencedNode != null) {
			document.put(NODEREFERECE_KEY, referencedNode.getUuid());
		}
		// TODO add disabled / enabled flag
		return document;
	}

	/**
	 * Add the given group uuid and names to the map.
	 * 
	 * @param document
	 * @param groups
	 */
	private void addGroups(JsonObject document, List<? extends Group> groups) {
		List<String> groupUuids = new ArrayList<>();
		List<String> groupNames = new ArrayList<>();
		for (Group group : groups) {
			groupUuids.add(group.getUuid());
			groupNames.add(group.getName());
		}
		Map<String, List<String>> groupFields = new HashMap<>();
		groupFields.put(UUID_KEY, groupUuids);
		groupFields.put(NAME_KEY, groupNames);
		document.put(GROUPS_KEY, groupFields);
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(USERNAME_KEY, trigramStringType());
		props.put(LASTNAME_KEY, trigramStringType());
		props.put(FIRSTNAME_KEY, trigramStringType());
		props.put(EMAIL_KEY, notAnalyzedType(STRING));
		props.put(NODEREFERECE_KEY, notAnalyzedType(STRING));
		props.put(GROUPS_KEY, new JsonObject()
			.put("type", OBJECT)
			.put("properties", new JsonObject()
				.put(NAME_KEY, trigramStringType())
				.put(UUID_KEY, notAnalyzedType(STRING))
			)
		);

		return props;
	}

}
