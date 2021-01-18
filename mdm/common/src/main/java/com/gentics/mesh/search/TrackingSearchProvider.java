package com.gentics.mesh.search;

import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.search.request.Bulkable;

import io.vertx.core.json.JsonObject;

/**
 * The {@link TrackingSearchProvider} is an extendes {@link SearchProvider} which has additional methods to access the tracked operations that were invoked
 * using this provider.
 */
public interface TrackingSearchProvider extends SearchProvider {

	/**
	 * Return the bulk requests.
	 * 
	 * @return
	 */
	List<Bulkable> getBulkRequests();

	/**
	 * Return the drop index events.
	 * 
	 * @return
	 */
	List<String> getDropIndexEvents();

	/**
	 * Return the create events.
	 * 
	 * @return
	 */
	Map<String, JsonObject> getCreateIndexEvents();

	/**
	 * Return the map of update events.
	 * 
	 * @return
	 */
	Map<String, JsonObject> getUpdateEvents();

	/**
	 * Return the delete events.
	 * 
	 * @return
	 */
	List<String> getDeleteEvents();

	/**
	 * Return the store events.
	 * 
	 * @return
	 */
	Map<String, JsonObject> getStoreEvents();

	/**
	 * Return the latest store event.
	 * 
	 * @param uuid
	 * @return
	 */
	JsonObject getLatestStoreEvent(String uuid);

}
