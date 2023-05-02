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

	private final String licenseKeyPath;


	public LicenseService(String licenseKey) {
		this.licenseKeyPath = licenseKey;
	}

	/**
	 * Checks if the feature has a valid license
	 *
	 * @return true if license is sufficient
	 */
	public boolean isLicenseSufficient(LicensedPlugin licensedPlugin) {
		if (!init()) {
			return false;
		}
		var isLicensed = LicenseChecker.getLicenseCheckResult().containsFeature(licensedPlugin);

		if (!isLicensed) {
			logger.error(
					String.format("Feature %s is not licensed", licensedPlugin.getProductName()));
		}

		return isLicensed;
	}


	/**
	 * Initialize the LicenseChecker. Reads the license.key file and register available features
	 *
	 * @return true if initializing was successfully (e.g. valid license.key file)
	 */
	private boolean init() {
		var licenseKeyFile = new File(this.licenseKeyPath);

		if (!licenseKeyFile.exists()) {
			logger.error(String.format("License key file does not exists at {%s}", this.licenseKeyPath));
			return false;
		}

		LicenseChecker.init(List.of(LicensedPlugin.values()), LicensedPlugin.MCP, licenseKeyFile,
				true,
				true);

		try {
			LicenseChecker.doLicenseCheck(true, null, null);
		}
		catch (Exception exception) {
			logger.error("License check failed. ", exception);
			return false;
		}

		return true;
	}

}
