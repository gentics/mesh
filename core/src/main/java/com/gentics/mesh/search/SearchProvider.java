package com.gentics.mesh.search;

import java.util.Map;

import org.elasticsearch.action.ActionResponse;
import org.elasticsearch.node.Node;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface SearchProvider {

	void refreshIndex();

	//TODO get rid of action response
	void updateDocument(String index, String type, String uuid, Map<String, Object> transformToDocumentMap,
			Handler<AsyncResult<ActionResponse>> handler);

	void deleteDocument(String index, String string, String uuid, Handler<AsyncResult<ActionResponse>> handler);

	void storeDocument(String index, String string, String uuid, Map<String, Object> map, Handler<AsyncResult<ActionResponse>> handler);

	void start();

	void stop();

	void reset();

	Node getNode();

}
