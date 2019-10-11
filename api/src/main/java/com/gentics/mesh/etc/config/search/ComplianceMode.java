package com.gentics.mesh.etc.config.search;

public enum ComplianceMode {

	/**
	 * Enables the pre Elasticsearch 7 compliance mode. This will allow Gentics Mesh to work with ES 6+ installations.
	 */
	ES_6,

	/**
	 * Enables the Elasticsearch 7 compliance mode. This will allow Gentics Mesh to work with ES 7 installations.
	 */
	ES_7
}
