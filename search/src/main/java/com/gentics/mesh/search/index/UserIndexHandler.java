package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.User;

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
	public void store(User user) {
		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, user);
		map.put("username", user.getUsername());
		map.put("emailadress", user.getEmailAddress());
		map.put("firstname", user.getFirstname());
		map.put("lastname", user.getLastname());
		//TODO add node reference?
		//TODO add disabled / enabled flag
		store(user.getUuid(), map);
	}

	@Override
	public void store(String uuid) {
		boot.userRoot().findByUuid(uuid, rh -> {
			if (rh.result() != null && rh.succeeded()) {
				User user = rh.result();
				store(user);
			} else {
				//TODO reply error? discard? log?
			}
		});
	}
}
