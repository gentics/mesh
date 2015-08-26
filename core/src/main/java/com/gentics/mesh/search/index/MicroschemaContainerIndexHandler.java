package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.action.ActionResponse;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

@Component
public class MicroschemaContainerIndexHandler extends AbstractIndexHandler<MicroschemaContainer> {

	@Override
	String getIndex() {
		return "microschema";
	}

	@Override
	String getType() {
		return "microschema";
	}

	@Override
	public void store(MicroschemaContainer microschema, Handler<AsyncResult<ActionResponse>> handler) {
		Map<String, Object> map = new HashMap<>();
		addBasicReferences(map, microschema);
		map.put("name", microschema.getName());
		store(microschema.getUuid(), map, handler);
	}

	@Override
	public void store(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		try (Trx tx = new Trx(db)) {
			boot.microschemaContainerRoot().findByUuid(uuid, rh -> {
				if (rh.result() != null && rh.succeeded()) {
					MicroschemaContainer microschema = rh.result();
					store(microschema, handler);
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
