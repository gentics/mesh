package com.gentics.mesh.plugin.common.license;


import com.gentics.license.LicenseChecker;
import com.gentics.mesh.plugin.common.MeshPluginException;
import java.io.File;
import java.util.List;

/**
 * Service to perform license checks. I.e. check which services are licensed and allowed to use.
 * This is a wrapper implementation around the LicenseChecker
 */
public class LicenseService {

	private final String licenseKeyPath;


	public LicenseService(String licenseKey) {
		this.licenseKeyPath = licenseKey;
	}

	/**
	 * Checks if the feature has a valid license
	 *
	 */
	public void isLicenseSufficient(LicensedPlugin licensedPlugin) throws MeshPluginException {
		init();
		var isLicensed = LicenseChecker.getLicenseCheckResult().containsFeature(licensedPlugin);

		if (!isLicensed) {
			throw new MeshPluginException(String.format("License insufficient. Plugin '%s' is not licensed", licensedPlugin.getProductName()));
		}
	}

	/**
	 * Initialize the LicenseChecker. Reads the license.key file and register available features
	 */
	private void init() throws MeshPluginException {
		var licenseKeyFile = new File(this.licenseKeyPath);

		if (!licenseKeyFile.exists()) {
			throw new MeshPluginException(String.format("License key file does not exists at {%s}", this.licenseKeyPath));
		}

		try {
			LicenseChecker.init(List.of(LicensedPlugin.values()), LicensedPlugin.MCP, licenseKeyFile,
					true,
					true);
			LicenseChecker.doLicenseCheck(true, null, null);
		}
		catch (Exception exception) {
			throw new MeshPluginException(String.format("License check failed. Reason: %s", exception.getMessage()));
		}
	}

}

