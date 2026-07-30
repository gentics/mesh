package com.gentics.mesh.test;

import com.gentics.mesh.dagger.SearchProviderType;

/**
 * Various test modes for ES tests.
 */
public enum ElasticsearchTestMode {

	/**
	 * Run with no ES support and set url to null
	 */
	NONE(0),

	/**
	 * Run with a fake tracking ES provider
	 */
	TRACKING(1),

	/**
	 * Run using an ES docker container
	 */
	CONTAINER_ES6(6),

	/**
	 * Run using an ES 7 docker container
	 */
	CONTAINER_ES7(7),

	/**
	 * Run using an ES 8 docker container
	 */
	CONTAINER_ES8(8),

	/**
	 * Run using an ES 9 docker container
	 */
	CONTAINER_ES9(9),

	/**
	 * Run using a toxified ES docker container
	 */
	CONTAINER_ES6_TOXIC(-6),

	/**
	 * Run using an ES docker container which is unreachable (listening on port 1)
	 */
	UNREACHABLE(2);

	private final int order;

	private ElasticsearchTestMode(int order) {
		this.order = order;
	}

	public SearchProviderType toSearchProviderType() {
		switch (this) {
			case NONE:
				return SearchProviderType.NULL;
			case TRACKING:
				return SearchProviderType.TRACKING;
			default:
				return SearchProviderType.ELASTICSEARCH;
		}
	}

	/**
	 * Get the ES version order.
	 * 
	 * @return
	 */
	public int getOrder() {
		return order;
	}
}
