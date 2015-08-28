package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.root.RootVertex;

@Component
public class RoleIndexHandler extends AbstractIndexHandler<Role> {

	@Override
	protected String getIndex() {
		return Role.TYPE;
	}

	@Override
	protected String getType() {
		return Role.TYPE;
	}

	@Override
	protected RootVertex<Role> getRootVertex() {
		return boot.meshRoot().getRoleRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(Role role) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", role.getName());
		addBasicReferences(map, role);
		return map;
	}
}
