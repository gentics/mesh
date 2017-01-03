package com.gentics.mesh.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.elasticsearch.node.Node;

import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.search.SearchProvider;

import io.vertx.core.json.JsonObject;
import rx.Completable;
import rx.Observable;
import rx.Single;

/**
 * Dummy search provider which just logs interacts with the search provider. This is useful when debugging or writing tests.
 */
public class DummySearchProvider implements SearchProvider {

	private Map<String, JsonObject> updateEvents = new HashMap<>();
	private List<String> deleteEvents = new ArrayList<>();
	private Map<String, JsonObject> storeEvents = new HashMap<>();
	private List<String> getEvents = new ArrayList<>();

	@Override
	public void refreshIndex(String... indices) {
	}

	@Override
	public Completable createIndex(String indexName) {
		return Completable.complete();
	}

	@Override
	public Completable updateDocument(String index, String type, String uuid, JsonObject document) {
		return Completable.fromAction(() -> {
			updateEvents.put(index + "-" + type + "-" + uuid, document);
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
	public Completable storeDocument(String index, String type, String uuid, JsonObject document) {
		return Completable.fromAction(() -> {
			storeEvents.put(index + "-" + type + "-" + uuid, document);
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

	public Map<String, JsonObject> getStoreEvents() {
		return storeEvents;
	}

	public List<String> getDeleteEvents() {
		return deleteEvents;
	}

	public Map<String, JsonObject> getUpdateEvents() {
		return updateEvents;
	}

	@Override
	public Completable clearIndex(String indexName) {
		return Completable.complete();
	}

	@Override
	public void clear() {
		updateEvents.clear();
		deleteEvents.clear();
		storeEvents.clear();
	}

	@Override
	public Completable deleteIndex(String indexName) {
		return Completable.complete();
	}

	@Override
	public Single<Integer> deleteDocumentsViaQuery(String index, String query) {
		return Single.just(0);
	}

	@Override
	public String getVendorName() {
		return "dummy";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

}
