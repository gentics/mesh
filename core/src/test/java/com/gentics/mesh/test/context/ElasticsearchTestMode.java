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
	 * Run using a ES docker container
	 */
	CONTAINER,
	/**
	 * Run using a ES docker container which includes the ingest plugin
	 */
	CONTAINER_WITH_INGEST;

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
