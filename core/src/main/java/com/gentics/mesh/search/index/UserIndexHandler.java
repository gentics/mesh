package com.gentics.mesh.search.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.RootVertex;

@Component
public class UserIndexHandler extends AbstractIndexHandler<User> {

	private static UserIndexHandler instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static UserIndexHandler getInstance() {
		return instance;
	}

	@Override
	protected String getIndex() {
		return User.TYPE;
	}

	@Override
	protected String getType() {
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
		map.put("username", user.getUsername());
		map.put("emailadress", user.getEmailAddress());
		map.put("firstname", user.getFirstname());
		map.put("lastname", user.getLastname());
		addGroups(map, user.getGroups());
		Node referencedNode = user.getReferencedNode();
		if (referencedNode != null) {
			map.put("nodeReference", referencedNode.getUuid());
		}
		//TODO add node reference?
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
		groupFields.put("uuid", groupUuids);
		groupFields.put("name", groupNames);
		map.put("groups", groupFields);
	}

}
