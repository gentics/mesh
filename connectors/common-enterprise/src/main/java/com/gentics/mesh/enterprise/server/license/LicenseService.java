package com.gentics.mesh.enterprise.server.license;

import com.gentics.license.LicenseChecker;
import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Service to perform license checks. I.e. check which services are licensed and allowed to use.
 * This is a wrapper implementation around the LicenseChecker
 */
public class LicenseService {

	protected static final Logger logger = LoggerFactory.getLogger(LicenseService.class);

	private final EnterpriseMeshOptions options;

	public LicenseService(EnterpriseMeshOptions options) {
		this.options = options;
	}

	/**
	 * Initialize the LicenseChecker. Reads the license.key file and register available features
	 *
	 * @return true if initializing was successfully (e.g. valid license.key file)
	 */
	private boolean init() {
		File licenseKeyFile = new File(options.getLicenseKeyPath());

		try {
			Optional<String> logMessage = LicenseChecker.createLicenseFileIfMissing(licenseKeyFile,
					options.getLicenseKey());
			logMessage.ifPresent(logger::info);
		} catch (Exception e) {
			logger.error(String.format("Error while writing license key to file %s",
					licenseKeyFile.getAbsolutePath()), e);
		}

		try {
			LicenseChecker.init(List.of(LicensedFeature.values()), LicensedFeature.MSQL, licenseKeyFile,
					true,
					true);

			LicenseChecker.doLicenseCheck(true, null, null);
		} catch (Exception exception) {
			logger.error("License check failed. ", exception);
			return false;
		}

		return true;
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
		boolean isLicensed = LicenseChecker.getLicenseCheckResult()
				.containsFeature(LicensedFeature.MSQL);

		if (!isLicensed) {
			logger.error("Feature {} database connector is not licensed", LicensedFeature.MSQL.getProductName());
		}

		return isLicensed;
	}

}

