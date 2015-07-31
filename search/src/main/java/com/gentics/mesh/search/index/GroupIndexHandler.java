package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Group;

@Component
public class GroupIndexHandler extends AbstractIndexHandler<Group> {

	@Override
	String getIndex() {
		return "group";
	}

	@Override
	String getType() {
		return "group";
	}

	@Override
	public void store(Group group) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", group.getName());
		addBasicReferences(map, group);
		//TODO addusers
		store(group.getUuid(),map);
	}

	@Override
	public void store(String uuid) {
		boot.groupRoot().findByUuid(uuid, rh -> {
			if (rh.result() != null && rh.succeeded()) {
				Group group = rh.result();
				store(group);
			} else {
				//TODO reply error? discard? log?
			}
		});
	}
}
