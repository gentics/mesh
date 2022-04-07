package com.gentics.mesh.test;

import com.gentics.mesh.dagger.SearchProviderType;

/**
 * Various test modes for ES tests.
 */
public enum ElasticsearchTestMode {

	/**
	 * Run with no ES support and set url to null
	 */
	NONE,

	/**
	 * Run with a fake tracking ES provider
	 */
	TRACKING,

	/**
	 * Run using an ES docker container
	 */
	CONTAINER_ES6,

	/**
	 * Run using an ES 7 docker container
	 */
	CONTAINER_ES7,

	/**
	 * Run using a toxified ES docker container
	 */
	CONTAINER_ES6_TOXIC,

	/**
	 * Run using an ES docker container which is unreachable (listening on port 1)
	 */
	UNREACHABLE;

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
}
