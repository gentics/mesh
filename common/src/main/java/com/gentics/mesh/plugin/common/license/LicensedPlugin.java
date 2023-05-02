package com.gentics.mesh.plugin.common.license;

import com.gentics.license.feature.LicenseKeyFeature;

/**
 * List of licensed features Mesh plugins
 */
public enum LicensedPlugin implements LicenseKeyFeature {

	MCMT("Mesh Comment Plugin"),

	MEXP("Mesh Export Plugin"),

	MFAV("Mesh Favourite Plugin"),

	MFRS("Mesh Forms Plugin"),

	GCMS("Mesh GCMS Plugin"),

	MKC("Mesh Keycloak Plugin"),

	MLKE("Mesh Like Plugin"),

	MMRD("Mesh Must Read Plugin"),

	MSRH("Mesh Search Plugin");


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
