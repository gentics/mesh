package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.NotImplementedException;
import org.elasticsearch.action.ActionResponse;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

@Component
public class TagIndexHandler extends AbstractIndexHandler<Tag> {

	@Override
	String getIndex() {
		return Tag.TYPE;
	}

	@Override
	String getType() {
		return Tag.TYPE;
	}

	public void store(Tag tag, Handler<AsyncResult<ActionResponse>> handler) {
		Map<String, Object> map = new HashMap<>();
		Map<String, String> tagFields = new HashMap<>();
		tagFields.put("name", tag.getName());
		map.put("fields", tagFields);
		addBasicReferences(map, tag);
		addTagFamily(map, tag.getTagFamily());
		store(tag.getUuid(), map, handler);
	}

	@Override
	public void store(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		try (Trx tx = db.trx()) {
			boot.tagRoot().findByUuid(uuid, rh -> {
				if (rh.result() != null && rh.succeeded()) {
					Tag tag = rh.result();
					store(tag, handler);
				} else {
					//TODO reply error? discard? log?
				}
			});
		}
	}

	private void addTagFamily(Map<String, Object> map, TagFamily tagFamily) {
		Map<String, Object> tagFamilyFields = new HashMap<>();
		tagFamilyFields.put("name", tagFamily.getName());
		tagFamilyFields.put("uuid", tagFamily.getUuid());
		map.put("tagFamily", tagFamilyFields);
	}

	public void update(String uuid, Handler<AsyncResult<ActionResponse>> handler) {
		throw new NotImplementedException();
	}
}
