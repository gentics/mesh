package com.gentics.mesh.test.context;

import com.gentics.mesh.dagger.SearchProviderType;

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
	 * Run with the embedded ES enabled
	 */
	EMBEDDED,

	/**
	 * Run using an ES docker container
	 */
	CONTAINER,

	/**
	 * Run using a toxified ES docker container
	 */
	CONTAINER_TOXIC,

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
