package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.action.ActionResponse;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

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
	public void store(Role role, Handler<AsyncResult<ActionResponse>> handler) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", role.getName());
		addBasicReferences(map, role);
		store(role.getUuid(), map, handler);
	}

	@Override
	public void store(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		try (Trx tx = new Trx(db)) {
			boot.roleRoot().findByUuid(uuid, rh -> {
				if (rh.result() != null && rh.succeeded()) {
					Role role = rh.result();
					store(role, handler);
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
