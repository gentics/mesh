package com.gentics.mesh.search;

import com.gentics.mesh.core.data.search.bulk.BulkEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.Bulkable;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Search provider which will return an error or <code>null</code> on most method calls.
 */
public class BrokenSearchProvider implements SearchProvider {

	private final Throwable error = new Exception("This search provider is intentionally broken");

	@Override
	public Completable refreshIndex(String... indices) {
		return Completable.error(error);
	}

	@Override
	public Single<Set<String>> listIndices() {
		return Single.error(error);
	}

	@Override
	public Completable createIndex(IndexInfo info) {
		return Completable.error(error);
	}

	@Override
	public Completable deregisterPipeline(String name) {
		return Completable.error(error);
	}

	@Override
	public Completable updateDocument(String indexName, String uuid, JsonObject document, boolean ignoreMissingDocumentError) {
		return Completable.error(error);
	}

	@Override
	public Completable deleteDocument(String indexName, String uuid) {
		return Completable.error(error);
	}

	@Override
	public Completable storeDocument(String indexName, String uuid, JsonObject document) {
		return Completable.error(error);
	}

	@Override
	public Completable processBulk(String actions) {
		return Completable.error(error);
	}

	@Override
	public Completable processBulkOld(List<? extends BulkEntry> entries) {
		return Completable.error(error);
	}

	@Override
	public Completable processBulk(Collection<? extends Bulkable> entries) {
		return Completable.error(error);
	}

	@Override
	public Single<JsonObject> getDocument(String indexName, String uuid) {
		return Single.error(error);
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public void reset() {
	}

	@Override
	public Completable clear() {
		return Completable.error(error);
	}

	@Override
	public Completable deleteIndex(boolean failOnMissingIndex, String... indexNames) {
		return Completable.error(error);
	}

	@Override
	public String getVendorName() {
		return "Broken";
	}

	@Override
	public String getVersion() {
		return "0";
	}

	@Override
	public SearchProvider init() {
		return this;
	}

	@Override
	public <T> T getClient() {
		return null;
	}

	@Override
	public JsonObject getDefaultIndexSettings() {
		return null;
	}

	@Override
	public Completable validateCreateViaTemplate(IndexInfo info) {
		return Completable.error(error);
	}

	@Override
	public String installationPrefix() {
		return "BROKEN";
	}

	@Override
	public Single<Boolean> isAvailable() {
		return Single.error(error);
	}

	@Override
	public boolean isActive() {
		return true;
	}
}
