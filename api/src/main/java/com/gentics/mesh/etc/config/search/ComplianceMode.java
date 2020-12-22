package com.gentics.mesh.etc.config.search;

/**
 * This enum keeps track of the different Elasticsearch compliance modes. Depending on the used ES version it is required to configure the specific mode.
 */
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
