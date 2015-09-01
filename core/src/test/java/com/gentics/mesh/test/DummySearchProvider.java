package com.gentics.mesh.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.node.Node;

import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;

public class DummySearchProvider implements SearchProvider {

	private List<String> updateEvents = new ArrayList<>();
	private List<String> deleteEvents = new ArrayList<>();
	private List<String> storeEvents = new ArrayList<>();

	@Override
	public void refreshIndex() {
		// TODO Auto-generated method stub
	}

	@Override
	public void updateDocument(String index, String type, String uuid, Map<String, Object> transformToDocumentMap,
			Handler<AsyncResult<Void>> handler) {
		updateEvents.add(index + "-" + type + "-" + uuid);
		handler.handle(Future.succeededFuture());
	}

	@Override
	public void deleteDocument(String index, String type, String uuid, Handler<AsyncResult<Void>> handler) {
		deleteEvents.add(index + "-" + type + "-" + uuid);
		handler.handle(Future.succeededFuture());
	}

	@Override
	public void storeDocument(String index, String type, String uuid, Map<String, Object> map, Handler<AsyncResult<Void>> handler) {
		storeEvents.add(index + "-" + type + "-" + uuid);
		handler.handle(Future.succeededFuture());
	}

	@Override
	public void start() {
		// TODO Auto-generated method stub

	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub

	}

	@Override
	public void reset() {
		updateEvents.clear();
		deleteEvents.clear();
		storeEvents.clear();
	}

	@Override
	public Node getNode() {
		// TODO Auto-generated method stub
		return null;
	}

	public List<String> getStoreEvents() {
		return storeEvents;
	}

	public List<String> getDeleteEvents() {
		return deleteEvents;
	}

	public List<String> getUpdateEvents() {
		return updateEvents;
	}

}
