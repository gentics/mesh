package com.gentics.mesh.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.json.JsonObject;
import rx.Completable;
import rx.Single;

/**
 * Dummy search provider which just logs interacts with the search provider. This is useful when debugging or writing tests.
 */
public class DummySearchProvider implements SearchProvider {

	private Map<String, JsonObject> updateEvents = new HashMap<>();
	private List<String> deleteEvents = new ArrayList<>();
	private Map<String, JsonObject> storeEvents = new HashMap<>();
	private List<String> getEvents = new ArrayList<>();
	private List<String> dropIndexEvents = new ArrayList<>();
	private List<String> createIndexEvents = new ArrayList<>();
	private Map<String, JsonObject> updateMappingEvents = new HashMap<>();

	@Override
	public SearchProvider init(MeshOptions options) {
		return this;
	}

	@Override
	public void refreshIndex(String... indices) {
	}

	@Override
	public Completable createIndex(String indexName, JsonObject settings, JsonObject mapping) {
		createIndexEvents.add(indexName);
		return Completable.complete();
	}

	@Override
	public Completable updateMapping(String indexName, String type, JsonObject mapping) {
		updateMappingEvents.put(indexName + "-" + type, mapping);
		return Completable.complete();
	}

	@Override
	public Completable updateDocument(String index, String type, String uuid, JsonObject document, boolean ignoreMissingDocumentError) {
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
	public Single<Map<String, Object>> getDocument(String index, String type, String uuid) {
		getEvents.add(index + "-" + type + "-" + uuid);
		return Single.just(null);
	}

	@Override
	public Completable storeDocumentBatch(String index, String type, Map<String, JsonObject> documents) {
		return Completable.complete();
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
	public Completable deleteIndex(String indexName, boolean failOnMissingIndex) {
		dropIndexEvents.add(indexName);
		return Completable.complete();
	}

	@Override
	public void reset() {
		clear();
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
		dropIndexEvents.clear();
		createIndexEvents.clear();
		updateMappingEvents.clear();
	}

	@Override
	public Single<Integer> deleteDocumentsViaQuery(String query, String... indices) {
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

	public Map<String, JsonObject> getStoreEvents() {
		return storeEvents;
	}

	public List<String> getDeleteEvents() {
		return deleteEvents;
	}

	public Map<String, JsonObject> getUpdateEvents() {
		return updateEvents;
	}

	public List<String> getCreateIndexEvents() {
		return createIndexEvents;
	}

	public List<String> getDropIndexEvents() {
		return dropIndexEvents;
	}

	public Map<String, JsonObject> getUpdateMappingEvents() {
		return updateMappingEvents;
	}

	@Override
	public <T> T getClient() {
		return null;
	}
}
