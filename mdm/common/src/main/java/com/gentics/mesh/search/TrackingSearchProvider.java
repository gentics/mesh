package com.gentics.mesh.search;

import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.search.request.Bulkable;

import io.vertx.core.json.JsonObject;

public interface TrackingSearchProvider extends SearchProvider {

	List<Bulkable> getBulkRequests();

	List<String> getDropIndexEvents();

	Map<String, JsonObject> getCreateIndexEvents();

	Map<String, JsonObject> getUpdateEvents();

	List<String> getDeleteEvents();

	Map<String, JsonObject> getStoreEvents();

}
