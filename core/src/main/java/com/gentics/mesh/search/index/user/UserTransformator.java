package com.gentics.mesh.search.index.user;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.search.index.AbstractTransformator;

import io.vertx.core.json.JsonObject;

/**
 * Transformator for user search index documents.
 */
public class UserTransformator extends AbstractTransformator<User> {

	public static final String EMAIL_KEY = "emailaddress";
	public static final String USERNAME_KEY = "username";
	public static final String FIRSTNAME_KEY = "firstname";
	public static final String LASTNAME_KEY = "lastname";

	@Override
	public JsonObject toDocument(User user) {
		JsonObject document = new JsonObject();
		addBasicReferences(document, user);
		document.put(USERNAME_KEY, user.getUsername());
		document.put(EMAIL_KEY, user.getEmailAddress());
		document.put(FIRSTNAME_KEY, user.getFirstname());
		document.put(LASTNAME_KEY, user.getLastname());
		addGroups(document, user.getGroups());
		Node referencedNode = user.getReferencedNode();
		if (referencedNode != null) {
			document.put("nodeReference", referencedNode.getUuid());
		}
		//TODO add disabled / enabled flag
		return document;
	}

	/**
	 * Add the given group uuid and names to the map.
	 * 
	 * @param map
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
		document.put("groups", groupFields);
	}

	@Override
	public JsonObject getMappingProperties() {
		JsonObject props = new JsonObject();
		props.put(LASTNAME_KEY, fieldType(STRING, NOT_ANALYZED));
		props.put(FIRSTNAME_KEY, fieldType(STRING, NOT_ANALYZED));
		props.put(EMAIL_KEY, fieldType(STRING, NOT_ANALYZED));
		return props;
	}

}
