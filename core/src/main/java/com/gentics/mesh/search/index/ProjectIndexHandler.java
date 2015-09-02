package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.RootVertex;

@Component
public class ProjectIndexHandler extends AbstractIndexHandler<Project> {

	private static ProjectIndexHandler instance;

	@PostConstruct
	public void setup() {
		instance = this;
	}

	public static ProjectIndexHandler getInstance() {
		return instance;
	}

	@Override
	protected String getIndex() {
		return Project.TYPE;
	}

	@Override
	protected String getType() {
		return Project.TYPE;
	}

	@Override
	protected RootVertex<Project> getRootVertex() {
		return boot.meshRoot().getProjectRoot();
	}

	@Override
	protected Map<String, Object> transformToDocumentMap(Project project) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", project.getName());
		addBasicReferences(map, project);
		return map;
	}

}
