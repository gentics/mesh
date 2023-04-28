package com.gentics.mesh.plugin.common.license;


import com.gentics.license.LicenseChecker;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.File;
import java.util.List;

/**
 * Service to perform license checks. I.e. check which services are licensed and allowed to use.
 * This is a wrapper implementation around the LicenseChecker
 */
public class LicenseService {

	private static final Logger logger = LoggerFactory.getLogger(LicenseService.class);

	private final String LICENSE_KEY_PATH;


	public LicenseService(String licenseKey) {
		this.LICENSE_KEY_PATH = licenseKey;
	}

	/**
	 * Checks if the feature has a valid license
	 *
	 * @return true if license is sufficient
	 */
	public boolean isLicenseSufficient() {
		if (!init()) {
			return false;
		}
		var isLicensed = LicenseChecker.getLicenseCheckResult().containsFeature(LicensedPlugin.MCP);

		if (!isLicensed) {
			logger.error(
					String.format("Feature %s is not licensed", LicensedPlugin.MCP.getProductName()));
		}

		return isLicensed;
	}


	/**
	 * Initialize the LicenseChecker. Reads the license.key file and register available features
	 *
	 * @return true if initializing was successfully (e.g. valid license.key file)
	 */
	private boolean init() {
		var licenseKeyFile = new File(this.LICENSE_KEY_PATH);

		if (!licenseKeyFile.exists()) {
			logger.error(String.format("License key file does not exists at {%s}", this.LICENSE_KEY_PATH));
			return false;
		}

		LicenseChecker.init(List.of(LicensedPlugin.values()), LicensedPlugin.MCP, licenseKeyFile,
				true,
				true);
		LicenseChecker.doLicenseCheck(true, null, null);

		return true;
	}

}
