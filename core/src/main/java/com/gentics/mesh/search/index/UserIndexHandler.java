package com.gentics.mesh.search.index;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.action.ActionResponse;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

@Component
public class UserIndexHandler extends AbstractIndexHandler<User> {

	@Override
	String getIndex() {
		return "user";
	}

	@Override
	String getType() {
		return "user";
	}

	@Override
	public void store(User user, Handler<AsyncResult<ActionResponse>> handler) {
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
		store(user.getUuid(), map, handler);
	}

	private void addGroups(Map<String, Object> map, List<? extends Group> groups) {
		List<String> groupUuids = new ArrayList<>();
		List<String> groupNames = new ArrayList<>();
		for (Group group : groups) {
			groupUuids.add(group.getUuid());
			groupNames.add(group.getName());
		}
		Map<String, List<String>> tagFields = new HashMap<>();
		tagFields.put("uuid", groupUuids);
		tagFields.put("name", groupNames);
		map.put("groups", tagFields);
	}

	@Override
	public void store(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
			boot.userRoot().findByUuid(uuid, rh -> {
				if (rh.result() != null && rh.succeeded()) {
					User user = rh.result();
					store(user, sh -> {

					});
				} else {
					//TODO reply error? discard? log?
				}
			});
	}

	public void update(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		// TODO Auto-generated method stub

	}
}
