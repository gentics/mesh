package com.gentics.mesh.search.index.user;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;

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
import com.gentics.mesh.util.ETag;

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
	public String generateVersion(User user) {
		return ETag.hash(toDocument(user, false).encode());
	}

	/**
	 * Transform the user to the document which can be stored in ES.
	 * 
	 * @param user
	 * @param withVersion
	 *            Whether to include the version number.
	 * @return
	 */
	private JsonObject toDocument(User user, boolean withVersion) {
		JsonObject document = new JsonObject();
		addBasicReferences(document, user);
		document.put(USERNAME_KEY, user.getUsername());
		document.put(EMAIL_KEY, user.getEmailAddress());
		document.put(FIRSTNAME_KEY, user.getFirstname());
		document.put(LASTNAME_KEY, user.getLastname());
		addGroups(document, user.getGroups());
		addPermissionInfo(document, user);
		// TODO add disabled / enabled flag
		Node referencedNode = user.getReferencedNode();
		if (referencedNode != null) {
			document.put(NODEREFERECE_KEY, referencedNode.getUuid());
		}
		if (withVersion) {
			document.put(VERSION_KEY, generateVersion(user));
		}
		return document;
	}

	@Override
	public JsonObject toDocument(User user) {
		return toDocument(user, true);
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
}
