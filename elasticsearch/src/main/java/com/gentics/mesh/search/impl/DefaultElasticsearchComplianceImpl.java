package com.gentics.mesh.search.impl;

import com.gentics.mesh.core.data.search.Compliance;
import com.gentics.mesh.etc.config.search.ComplianceMode;

/**
 * A default Elasticsearch compliance implementation.
 */
public class DefaultElasticsearchComplianceImpl implements Compliance {

	private final ComplianceMode mode;

	public DefaultElasticsearchComplianceImpl(ComplianceMode mode) {
		this.mode = mode;
	}

	@Override
	public ComplianceMode getMode() {
		return mode;
	}
}
