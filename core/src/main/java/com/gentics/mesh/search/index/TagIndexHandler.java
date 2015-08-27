package com.gentics.mesh.search.index;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.action.ActionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.graphdb.Trx;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
public class TagIndexHandler extends AbstractIndexHandler<Tag> {

	private static final Logger log = LoggerFactory.getLogger(TagIndexHandler.class);

	@Autowired
	private NodeIndexHandler nodeIndexHandler;

	@Autowired
	private TagFamilyIndexHandler tagFamilyIndexHandler;

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
					log.error("Could not store tag {" + uuid + "}. Tag could not be loaded.", rh.cause());
					handler.handle(Future.failedFuture(rh.cause()));
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
		try (Trx tx = db.trx()) {
			boot.tagRoot().findByUuid(uuid, rh -> {
				if (rh.result() != null && rh.succeeded()) {
					Tag tag = rh.result();
//					tag.updateIndex();
					// 1. Store the tag 
					store(tag, handler);
					// 2. Update all nodes that use the tag by storing them again
					for (Node node : tag.getNodes()) {
						String nodeUuid= node.getUuid();
						nodeIndexHandler.store(nodeUuid, rhn -> {
							if (rhn.succeeded()) {
								log.info("Updated node {" + nodeUuid + "}");
							}
							//TODO log error
						});
					}
					// 3. Update the tag family of the tag by storing it again
					String tagFamilyUuid = tag.getTagFamily().getUuid();
					tagFamilyIndexHandler.store(tagFamilyUuid, rht -> {
						if (rht.succeeded()) {
							log.info("Updated tagFamily {" + tagFamilyUuid + "}");
						}
						//TODO log error
					});
				} else {
					log.error("Could not store tag {" + uuid + "}. Tag could not be loaded.", rh.cause());
					handler.handle(Future.failedFuture(rh.cause()));
				}
			});
		}

	}
}
