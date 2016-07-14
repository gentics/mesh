package com.gentics.mesh.search.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.node.Node;

import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.search.SearchProvider;

import rx.Completable;
import rx.Observable;
import rx.Single;

public class DummySearchProvider implements SearchProvider {

	private Map<String, Map<String, Object>> updateEvents = new HashMap<>();
	private List<String> deleteEvents = new ArrayList<>();
	private Map<String, Map<String, Object>> storeEvents = new HashMap<>();
	private List<String> getEvents = new ArrayList<>();

	@Override
	public void refreshIndex() {
	}

	@Override
	public Completable createIndex(String indexName) {
		return Completable.complete();
	}

	@Override

	public Completable updateDocument(String index, String type, String uuid, Map<String, Object> map) {
		return Completable.fromAction(() -> {
			updateEvents.put(index + "-" + type + "-" + uuid, map);
		});
	}

	public Completable setNodeIndexMapping(String indexName, String type, Schema schema) {
		return Completable.complete();
	}

	@Override
	public Completable deleteDocument(String index, String type, String uuid) {
		return Completable.fromAction(() -> {
			deleteEvents.add(index + "-" + type + "-" + uuid);
		});
	}

	@Override
	public Observable<Map<String, Object>> getDocument(String index, String type, String uuid) {
		getEvents.add(index + "-" + type + "-" + uuid);
		return Observable.just(null);
	}

	@Override
	public Completable storeDocument(String index, String type, String uuid, Map<String, Object> map) {
		return Completable.fromAction(() -> {
			storeEvents.put(index + "-" + type + "-" + uuid, map);
		});
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
	public Completable clearIndex(String indexName) {
		return Completable.complete();
	}

	@Override
	public Completable deleteIndex(String indexName) {
		return Completable.complete();
	}

	@Override
	public Single<Integer> deleteDocumentsViaQuery(String index, String query) {
		return Single.just(0);
	}
}
