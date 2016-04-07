package com.gentics.mesh.search.index;

import static com.gentics.mesh.search.index.MappingHelper.NAME_KEY;
import static com.gentics.mesh.search.index.MappingHelper.NOT_ANALYZED;
import static com.gentics.mesh.search.index.MappingHelper.STRING;
import static com.gentics.mesh.search.index.MappingHelper.UUID_KEY;
import static com.gentics.mesh.search.index.MappingHelper.fieldType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueueEntry;

import io.vertx.core.json.JsonObject;

@Component
public class UserIndexHandler extends AbstractIndexHandler<User> {

	public static final String EMAIL_KEY = "emailaddress";
	public static final String USERNAME_KEY = "username";
	public static final String FIRSTNAME_KEY = "firstname";
	public static final String LASTNAME_KEY = "lastname";

	private final static Set<String> indices = Collections.singleton(User.TYPE);

	private static UserIndexHandler instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static UserIndexHandler getInstance() {
		return instance;
	}

	@Override
	protected String getIndex(SearchQueueEntry entry) {
		return User.TYPE;
	}

	@Override
	public Set<String> getIndices() {
		return indices;
	}

	@Override
	public Set<String> getAffectedIndices(InternalActionContext ac) {
		return indices;
	}

	@Override
	protected String getType() {
		return User.TYPE;
	}

	@Override
	public String getKey() {
		return User.TYPE;
	}

	@Override
	protected RootVertex<User> getRootVertex() {
		return boot.meshRoot().getUserRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(User user) {
		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, user);
		map.put(USERNAME_KEY, user.getUsername());
		map.put(EMAIL_KEY, user.getEmailAddress());
		map.put(FIRSTNAME_KEY, user.getFirstname());
		map.put(LASTNAME_KEY, user.getLastname());
		addGroups(map, user.getGroups());
		Node referencedNode = user.getReferencedNode();
		if (referencedNode != null) {
			map.put("nodeReference", referencedNode.getUuid());
		}
		//TODO add disabled / enabled flag
		return map;
	}

	/**
	 * Add the given group uuid and names to the map.
	 * 
	 * @param map
	 * @param groups
	 */
	private void addGroups(Map<String, Object> map, List<? extends Group> groups) {
		List<String> groupUuids = new ArrayList<>();
		List<String> groupNames = new ArrayList<>();
		for (Group group : groups) {
			groupUuids.add(group.getUuid());
			groupNames.add(group.getName());
		}
		Map<String, List<String>> groupFields = new HashMap<>();
		groupFields.put(UUID_KEY, groupUuids);
		groupFields.put(NAME_KEY, groupNames);
		map.put("groups", groupFields);
	}

	@Override
	protected JsonObject getMapping() {
		JsonObject props = new JsonObject();
		props.put(LASTNAME_KEY, fieldType(STRING, NOT_ANALYZED));
		props.put(FIRSTNAME_KEY, fieldType(STRING, NOT_ANALYZED));
		props.put(EMAIL_KEY, fieldType(STRING, NOT_ANALYZED));
		return props;
	}

}
