package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Project;

@Component
public class ProjectIndexHandler extends AbstractIndexHandler<Project> {

	@Override
	String getIndex() {
		return "project";
	}

	@Override
	String getType() {
		return "project";
	}

	@Override
	public void store(Project project) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", project.getName());
		addBasicReferences(map, project);
		store(project.getUuid(), map);
	}

	@Override
	public void store(String uuid) {
		boot.projectRoot().findByUuid(uuid, rh -> {
			if (rh.result() != null && rh.succeeded()) {
				Project project = rh.result();
				store(project);
			} else {
				//TODO reply error? discard? log?
			}
		});

	}

	public void update(String uuid) {
		// TODO Auto-generated method stub
		
	}

}
