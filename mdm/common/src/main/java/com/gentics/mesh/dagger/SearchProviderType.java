package com.gentics.mesh.dagger;

/**
 * Types of search providers.
 */
public enum SearchProviderType {

	/**
	 * Dev Null provider which does NOOP's
	 */
	NULL,

	/**
	 * Provider which tracks the operations but does not use ES.
	 */
	TRACKING,

	/**
	 * Provider which uses Elasticsearch.
	 */
	ELASTICSEARCH
}
