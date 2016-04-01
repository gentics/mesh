package com.gentics.mesh.search.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.node.Node;

import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.search.SearchProvider;

import rx.Observable;

public class DummySearchProvider implements SearchProvider {

	private Map<String, Map<String, Object>> updateEvents = new HashMap<>();
	private List<String> deleteEvents = new ArrayList<>();
	private Map<String, Map<String, Object>> storeEvents = new HashMap<>();
	private List<String> getEvents = new ArrayList<>();

	@Override
	public void refreshIndex() {
	}

	@Override
	public Observable<Void> createIndex(String indexName) {
		return Observable.just(null);
	}

	@Override

	public Observable<Void> updateDocument(String index, String type, String uuid, Map<String, Object> map) {
		updateEvents.put(index + "-" + type + "-" + uuid, map);
		return Observable.just(null);
	}

	public Observable<Void> setNodeIndexMapping(String indexName, String type, Schema schema) {
		return Observable.just(null);
	}

	@Override
	public Observable<Void> deleteDocument(String index, String type, String uuid) {
		deleteEvents.add(index + "-" + type + "-" + uuid);
		return Observable.just(null);
	}

	@Override
	public Observable<Map<String, Object>> getDocument(String index, String type, String uuid) {
		getEvents.add(index + "-" + type + "-" + uuid);
		return Observable.just(null);
	}

	@Override
	public Observable<Void> storeDocument(String index, String type, String uuid, Map<String, Object> map) {
		storeEvents.put(index + "-" + type + "-" + uuid, map);
		return Observable.just(null);
	}

	@Override
	public void start() {

	}

	@Override
	public void stop() {
	}

	@Override
	public void reset() {
		updateEvents.clear();
		deleteEvents.clear();
		storeEvents.clear();
	}

	@Override
	public Node getNode() {
		return null;
	}

	public Map<String, Map<String, Object>> getStoreEvents() {
		return storeEvents;
	}

	public List<String> getDeleteEvents() {
		return deleteEvents;
	}

	public Map<String, Map<String, Object>> getUpdateEvents() {
		return updateEvents;
	}

	@Override
	public Observable<Void> clearIndex(String indexName) {
		return Observable.just(null);
	}

	@Override
	public Observable<Void> deleteIndex(String indexName) {
		return Observable.just(null);
	}

	@Override
	public Observable<Integer> deleteDocumentsViaQuery(String index, String query) {
		return Observable.just(0);
	}
}
