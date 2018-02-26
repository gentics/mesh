package com.gentics.mesh.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.etc.config.MeshOptions;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

/**
 * Search provider which just logs interacts with the search provider. This is useful when debugging or writing tests.
 */
public class TrackingSearchProvider implements SearchProvider {

	private Map<String, JsonObject> updateEvents = new HashMap<>();
	private List<String> deleteEvents = new ArrayList<>();
	private Map<String, JsonObject> storeEvents = new HashMap<>();
	private List<String> getEvents = new ArrayList<>();
	private List<String> dropIndexEvents = new ArrayList<>();
	private Map<String, JsonObject> createIndexEvents = new HashMap<>();

	@Override
	public SearchProvider init(MeshOptions options) {
		return this;
	}

	@Override
	public JsonObject getDefaultIndexSettings() {
		return new JsonObject();
	}

	@Override
	public Completable refreshIndex(String... indices) {
		return Completable.complete();
	}

	@Override
	public Completable createIndex(IndexInfo info) {
		JsonObject json = new JsonObject();
		json.put("mapping", info.getIndexMappings());
		json.put("settings", info.getIndexSettings());
		createIndexEvents.put(info.getIndexName(), json);
		return Completable.complete();
	}

	@Override
	public Completable updateDocument(String index, String uuid, JsonObject document, boolean ignoreMissingDocumentError) {
		return Completable.fromAction(() -> updateEvents.put(index + "-" + uuid, document));
	}

	public Completable setNodeIndexMapping(String indexName, String type, Schema schema) {
		return Completable.complete();
	}

	@Override
	public Completable deleteDocument(String index, String uuid) {
		return Completable.fromAction(() -> deleteEvents.add(index + "-" + uuid));
	}

	@Override
	public Single<JsonObject> getDocument(String index, String uuid) {
		getEvents.add(index + "-" + uuid);
		return Single.just(new JsonObject());
	}

	@Override
	public Completable storeDocumentBatch(String index, Map<String, JsonObject> documents) {
		return Completable.complete();
	}

	@Override
	public Completable storeDocument(String index, String uuid, JsonObject document) {
		return Completable.fromAction(() -> storeEvents.put(index + "-" + uuid, document));
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public Completable deleteIndex(boolean failOnMissingIndex, String... indexNames) {
		for (String indexName : indexNames) {
			dropIndexEvents.add(indexName);
		}
		return Completable.complete();
	}

	@Override
	public void reset() {
		clear().blockingAwait();
	}

	@Override
	public Completable clear() {
		updateEvents.clear();
		deleteEvents.clear();
		storeEvents.clear();
		dropIndexEvents.clear();
		createIndexEvents.clear();
		return Completable.complete();
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

	public Map<String, JsonObject> getCreateIndexEvents() {
		return createIndexEvents;
	}

	public List<String> getDropIndexEvents() {
		return dropIndexEvents;
	}

	@Override
	public Completable validateCreateViaTemplate(IndexInfo info) {
		return Completable.complete();
	}

	@Override
	public <T> T getClient() {
		return null;
	}

	public void printStoreEvents(boolean withJson) {
		for (Entry<String, JsonObject> entry : getStoreEvents().entrySet()) {
			System.out.println("Store event {" + entry.getKey() + "}");
			if (withJson) {
				System.out.println("Json:\n" + entry.getValue().encodePrettily());
			}
		}

	}
}
