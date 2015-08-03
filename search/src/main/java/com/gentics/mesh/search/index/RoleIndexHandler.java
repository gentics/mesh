package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Role;

@Component
public class RoleIndexHandler extends AbstractIndexHandler<Role> {

	@Override
	String getIndex() {
		return "role";
	}

	@Override
	String getType() {
		return "role";
	}

	@Override
	public void store(Role role) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", role.getName());
		addBasicReferences(map, role);
		store(role.getUuid(), map);
	}

	@Override
	public void store(String uuid) {
		boot.roleRoot().findByUuid(uuid, rh -> {
			if (rh.result() != null && rh.succeeded()) {
				Role role = rh.result();
				store(role);
			} else {
				//TODO reply error? discard? log?
			}
		});

	}

	public void update(String uuid) {
		// TODO Auto-generated method stub
		
	}

}
