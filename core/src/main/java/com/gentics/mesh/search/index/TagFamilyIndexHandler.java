package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.ActionResponse;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

@Component
public class TagFamilyIndexHandler extends AbstractIndexHandler<TagFamily> {

	@Override
	String getIndex() {
		return "tag_family";
	}

	@Override
	String getType() {
		return "tagFamily";
	}

	@Override
	public void store(TagFamily tagFamily, Handler<AsyncResult<ActionResponse>> handler) {
		Map<String, Object> map = new HashMap<>();
		map.put("name", tagFamily.getName());
		addBasicReferences(map, tagFamily);
		addTags(map, tagFamily.getTags());
		store(tagFamily.getUuid(), map, handler);
	}

	@Override
	public void store(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		try (Trx tx = new Trx(db)) {
			boot.tagFamilyRoot().findByUuid(uuid, rh -> {
				if (rh.result() != null && rh.succeeded()) {
					TagFamily tagFamily = rh.result();
					store(tagFamily, handler);
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
