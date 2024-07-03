package com.gentics.mesh.enterprise.server.license;


import com.gentics.license.feature.LicenseKeyFeature;

/**
 * Collection of licensed features of Mesh
 */
public enum LicensedFeature implements LicenseKeyFeature {

	MSQL("Gentics Mesh SQL");

	private final String productName;

	LicensedFeature(String productName) {
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

