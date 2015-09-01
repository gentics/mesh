package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.root.RootVertex;

@Component
public class GroupIndexHandler extends AbstractIndexHandler<Group> {

	private static GroupIndexHandler instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static GroupIndexHandler getInstance() {
		return instance;
	}

	@Override
	protected String getIndex() {
		return "group";
	}

	@Override
	protected String getType() {
		return "group";
	}

	@Override
	protected RootVertex<Group> getRootVertex() {
		return boot.meshRoot().getGroupRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(Group group) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", group.getName());
		addBasicReferences(map, group);
		return map;
	}
}
