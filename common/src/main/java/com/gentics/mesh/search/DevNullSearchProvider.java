package com.gentics.mesh.search;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.Bulkable;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.etc.config.MeshOptions;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;

/**
 * Search provider which will accept any action without actually doing anything.
 */
public class DevNullSearchProvider implements SearchProvider {

	private MeshOptions options;

	public DevNullSearchProvider(MeshOptions options) {
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
	public Completable createIndex(IndexInfo info) {
		return Completable.complete();
	}

	@Override
	public Completable deregisterPipeline(String name) {
		return Completable.complete();
	}

	@Override
	public Completable updateDocument(String index, String uuid, JsonObject document, boolean ignoreMissingDocumentError) {
		return Completable.complete();
	}

	public Completable setNodeIndexMapping(String indexName, String type, Schema schema) {
		return Completable.complete();
	}

	@Override
	public Completable deleteDocument(String index, String uuid) {
		return Completable.complete();
	}

	@Override
	public Single<Set<String>> listIndices() {
		return Single.just(Collections.emptySet());
	}

	@Override
	public Single<JsonObject> getDocument(String index, String uuid) {
		return Single.just(new JsonObject());
	}

	@Override
	public Completable processBulk(Collection<? extends Bulkable> entries) {
		return Completable.complete();
	}

	@Override
	public Completable storeDocument(String index, String uuid, JsonObject document) {
		return Completable.complete();
	}

	@Override
	public Completable processBulk(String actions) {
		return Completable.complete();
	}

	@Override
	public void start() {
	}

	@Override
	public void stop() {
	}

	@Override
	public Completable deleteIndex(boolean failOnMissingIndex, String... indexNames) {
		return Completable.complete();
	}

	@Override
	public void reset() {
	}

	@Override
	public Completable clear(String indexPattern) {
		return Completable.complete();
	}

	@Override
	public String getVendorName() {
		return "dev-null";
	}

	@Override
	public String getVersion() {
		return "1.0";
	}

	@Override
	public Completable validateCreateViaTemplate(IndexInfo info) {
		return Completable.complete();
	}

	@Override
	public <T> T getClient() {
		return null;
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
		// This provider is always inactive
		return false;
	}

}
