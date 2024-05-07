package com.gentics.mesh.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.BulkRequest;
import com.gentics.mesh.core.data.search.request.Bulkable;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.DeleteDocumentRequest;
import com.gentics.mesh.core.data.search.request.UpdateDocumentRequest;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.util.VersionNumber;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Search provider which just logs interacts with the search provider. This is useful when debugging or writing tests.
 */
public class TrackingSearchProviderImpl implements TrackingSearchProvider {

	private static final Logger log = LoggerFactory.getLogger(TrackingSearchProviderImpl.class);

	public static final String TEST_PROPERTY_KEY = "mesh.test";

	private Map<String, JsonObject> updateEvents = new HashMap<>();
	private List<String> deleteEvents = new ArrayList<>();
	private Map<String, JsonObject> storeEvents = new HashMap<>();
	private List<String> getEvents = new ArrayList<>();
	private List<String> dropIndexEvents = new ArrayList<>();
	private Map<String, JsonObject> createIndexEvents = new HashMap<>();
	private List<Bulkable> bulkRequests = new ArrayList<>();

	private MeshOptions options;

	public TrackingSearchProviderImpl(MeshOptions options) {
		this.options = options;
	}

	@Override
	public SearchProvider init() {
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
	public Single<Set<String>> listIndices() {
		return Single.just(Collections.emptySet());
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
	public Completable deregisterPipeline(String name) {
		return Completable.complete();
	}

	@Override
	public Completable updateDocument(String index, String uuid, JsonObject document, boolean ignoreMissingDocumentError) {
		return Completable.fromAction(() -> {
			updateEvents.put(index + "-" + uuid, document);
		});
	}

	/**
	 * No-operation setter for the index mapping. The method will just complete.
	 * 
	 * @param indexName
	 * @param type
	 * @param schema
	 * @return
	 */
	public Completable setNodeIndexMapping(String indexName, String type, SchemaModel schema) {
		return Completable.complete();
	}

	@Override
	public Completable deleteDocument(String index, String uuid) {
		return Completable.fromAction(() -> {
			deleteEvents.add(index + "-" + uuid);
		});
	}

	@Override
	public Single<JsonObject> getDocument(String index, String uuid) {
		getEvents.add(index + "-" + uuid);
		return Single.just(new JsonObject());
	}

	@Override
	public Completable processBulk(Collection<? extends Bulkable> entries) {
		for (Bulkable entry : entries) {
			if (entry instanceof CreateDocumentRequest) {
				CreateDocumentRequest request = (CreateDocumentRequest) entry;
				storeEvents.put(request.getIndex() + "-" + request.getId(), request.getDoc());
			} else if (entry instanceof DeleteDocumentRequest) {
				DeleteDocumentRequest request = (DeleteDocumentRequest) entry;
				deleteEvents.add(request.getIndex() + "-" + request.getId());
			} else if (entry instanceof UpdateDocumentRequest) {
				UpdateDocumentRequest request = (UpdateDocumentRequest) entry;
				updateEvents.put(request.getIndex() + "-" + request.getId(), request.getDoc());
			} else if (entry instanceof BulkRequest) {
				BulkRequest request = (BulkRequest) entry;
				processBulk(request.getRequests());
			} else {
				log.warn("Unknown bulkable request found: {}", entry);
			}
		}
		bulkRequests.addAll(entries);
		return Completable.complete();
	}

	@Override
	public Completable processBulk(String actions) {
		return Completable.complete();
	}

	@Override
	public Completable storeDocument(String index, String uuid, JsonObject document) {
		return Completable.fromAction(() -> {
			storeEvents.put(index + "-" + uuid, document);
		});
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
	public Completable clear(String indexPattern) {
		updateEvents.clear();
		deleteEvents.clear();
		storeEvents.clear();
		dropIndexEvents.clear();
		createIndexEvents.clear();
		bulkRequests.clear();
		return Completable.complete();
	}

	@Override
	public String getVendorName() {
		return "dummy";
	}

	@Override
	public String getVersion(boolean failIfNotAvailable) {
		return "1.0";
	}

	@Override
	public Map<String, JsonObject> getStoreEvents() {
		return storeEvents;
	}

	@Override
	public JsonObject getLatestStoreEvent(String uuid) {
		Map<String, JsonObject> storeEvents = getStoreEvents();
		return storeEvents.values().stream()
			.filter(json -> json.getString("uuid").equals(uuid))
			.max(Comparator.comparing(json -> new VersionNumber(json.getString("version"))))
			.orElse(null);
	}

	@Override
	public List<String> getDeleteEvents() {
		return deleteEvents;
	}

	@Override
	public Map<String, JsonObject> getUpdateEvents() {
		return updateEvents;
	}

	@Override
	public Map<String, JsonObject> getCreateIndexEvents() {
		return createIndexEvents;
	}

	@Override
	public List<String> getDropIndexEvents() {
		return dropIndexEvents;
	}

	@Override
	public List<Bulkable> getBulkRequests() {
		return bulkRequests;
	}

	@Override
	public Completable validateCreateViaTemplate(IndexInfo info) {
		return Completable.complete();
	}

	@Override
	public <T> T getClient() {
		return null;
	}

	/**
	 * Print the tracked store events to the console.
	 * 
	 * @param withJson
	 *            Flag to control whether the entry json should also be printed
	 */
	public void printStoreEvents(boolean withJson) {
		for (Entry<String, JsonObject> entry : getStoreEvents().entrySet()) {
			log.debug("Store event {" + entry.getKey() + "}");
			if (withJson) {
				log.info("Json:\n" + entry.getValue().encodePrettily());
			}
		}
	}

	@Override
	public String installationPrefix() {
		return options.getSearchOptions().getPrefix();
	}

	@Override
	public Single<Boolean> isAvailable() {
		return Single.just(false);
	}

	@Override
	public boolean isActive() {
		return true;
	}
}
