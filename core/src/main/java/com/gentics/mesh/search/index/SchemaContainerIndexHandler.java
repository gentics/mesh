package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.action.ActionResponse;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

@Component
public class SchemaContainerIndexHandler extends AbstractIndexHandler<SchemaContainer> {

	@Override
	String getIndex() {
		return "schema_container";
	}

	@Override
	String getType() {
		return "schemaContainer";
	}

	@Override
	public void store(SchemaContainer container, Handler<AsyncResult<ActionResponse>> handler) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", container.getName());
		addBasicReferences(map, container);
		store(container.getUuid(), map, handler);
	}

	@Override
	public void store(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		try (Trx tx = db.trx()) {
			boot.schemaContainerRoot().findByUuid(uuid, rh -> {
				if (rh.result() != null && rh.succeeded()) {
					SchemaContainer container = rh.result();
					store(container, handler);
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
