package com.gentics.mesh.metric;

/**
 * Metrics for search requests
 */
public enum SearchRequestMetric implements Metric {
	/**
	 * Number of currently open index requests
	 */
	REQUESTS("index_requests_open", "Number of open index requests."),

	/**
	 * Numer of currently open transformations
	 */
	TRANSFORMATIONS("index_transformations_open", "Number of open transformations."),

	/**
	 * 1 if the search is idle, 0 if it is busy
	 */
	IDLE("search_idle", "Search is idle"),

	/**
	 * Numer of buffered index events
	 */
	BUFFERED("index_buffered_events", "Number of currently buffered events."),

	/**
	 * Number of currently open search (or graphql) requests waiting for the search to become idle
	 */
	WAITING("search_requests_waiting", "Number of search requests waiting for idle."),

	/**
	 * Waiting time for the search to become idle
	 */
	WAITING_TIME("search_requests_wait_time", "Search request waiting time."),

	/**
	 * Number of currently open store requests
	 */
	STORE("index_store", "Current store requests");

	private String key;

	private String description;

	/**
	 * Create instance
	 * @param key metric key
	 * @param description metric description
	 */
	private SearchRequestMetric(String key, String description) {
		this.key = key;
		this.description = description;
	}

	@Override
	public String key() {
		return "mesh_" + key;
	}

	@Override
	public String description() {
		return description;
	}

}
