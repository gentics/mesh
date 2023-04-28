package com.gentics.mesh.plugin.common.license;

import com.gentics.license.feature.LicenseKeyFeature;

/**
 * List of licensed features Mesh plugins
 */
public enum LicensedPlugin implements LicenseKeyFeature {

	MCP("Mesh Comment Plugin");

	private final String productName;

	LicensedPlugin(String productName) {
		this.productName = productName;
	}

	@Override
	public String getProductName() {
		return this.productName;
	}

	@Override
	public String getVersion() {
		return null;
	}

}
