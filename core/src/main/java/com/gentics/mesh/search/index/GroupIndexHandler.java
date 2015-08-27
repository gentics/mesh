package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.action.ActionResponse;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

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
	public void store(Group group, Handler<AsyncResult<ActionResponse>> handler) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", group.getName());
		addBasicReferences(map, group);
		//TODO addusers
		store(group.getUuid(), map, handler);
	}

	@Override
	public void store(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		try (Trx tx = db.trx()) {
			boot.groupRoot().findByUuid(uuid, rh -> {
				if (rh.result() != null && rh.succeeded()) {
					Group group = rh.result();
					store(group, handler);
				} else {
					//TODO reply error? discard? log?
				}
			});
		}
	}

	public void update(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		throw new NotImplementedException();
	}
}
