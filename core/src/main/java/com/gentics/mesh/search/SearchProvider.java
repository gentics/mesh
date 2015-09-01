package com.gentics.mesh.search;

import java.util.Map;

import org.elasticsearch.node.Node;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface SearchProvider {

	void refreshIndex();

	//TODO add a good response instead of void. We need this in oder to handle correct logging?
	void updateDocument(String index, String type, String uuid, Map<String, Object> transformToDocumentMap, Handler<AsyncResult<Void>> handler);

	void deleteDocument(String index, String type, String uuid, Handler<AsyncResult<Void>> handler);

	void storeDocument(String index, String type, String uuid, Map<String, Object> map, Handler<AsyncResult<Void>> handler);

	void start();

	void stop();

	void reset();

	Node getNode();

}
