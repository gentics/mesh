package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.ActionResponse;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

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
	public void store(Project project, Handler<AsyncResult<ActionResponse>> handler) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", project.getName());
		addBasicReferences(map, project);
		store(project.getUuid(), map, handler);
	}

	@Override
	public void store(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		try (Trx tx = new Trx(db)) {
			boot.projectRoot().findByUuid(uuid, rh -> {
				if (rh.result() != null && rh.succeeded()) {
					Project project = rh.result();
					store(project, handler);
				} else {
					//TODO reply error? discard? log?
				}
			});
		}

	}

	public void update(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		// TODO Auto-generated method stub

	}

}
