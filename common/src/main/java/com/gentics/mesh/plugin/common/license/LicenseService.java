package com.gentics.mesh.plugin.common.license;

import com.gentics.license.LicenseChecker;
import com.gentics.mesh.plugin.common.MeshPluginException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Service to perform license checks. I.e. check which services are licensed and allowed to use.
 * This is a wrapper implementation around the LicenseChecker
 */
public class LicenseService {

	private static final Logger logger = LoggerFactory.getLogger(LicenseService.class);

	private final LicenseConfiguration config;

	public LicenseService(LicenseConfiguration config) {
		this.config = config;
	}

	/**
	 * Initialize the LicenseChecker. Reads the license.key file and register available features
	 *
	 * @param licensedPlugin the plugin to initialize
	 */
	private void init(LicensedPlugin licensedPlugin) throws MeshPluginException {
		var licenseKeyFile = new File(config.getLicenseKeyPath());

		try {
			Optional<String> logMessage = LicenseChecker.createLicenseFileIfMissing(licenseKeyFile,
					config.getLicenseKey());
			logMessage.ifPresent(logger::info);
		} catch (Exception e) {
			logger.error(String.format("Error while writing license key to file %s",
					licenseKeyFile.getAbsolutePath()), e);
		}

		try {
			LicenseChecker.init(List.of(LicensedPlugin.values()), licensedPlugin, licenseKeyFile,
					true,
					true);
			LicenseChecker.doLicenseCheck(true, null, null);
		} catch (Exception exception) {
			throw new MeshPluginException(
					String.format("License check failed. Reason: %s", exception.getMessage()));
		}
	}


	/**
	 * Checks if the feature has a valid license
	 *
	 * @param licensedPlugin the plugin to check for a valid license
	 */
	public void isLicenseSufficient(LicensedPlugin licensedPlugin) throws MeshPluginException {
		init(licensedPlugin);
		var isLicensed = LicenseChecker.getLicenseCheckResult().containsFeature(licensedPlugin);

		if (!isLicensed) {
			throw new MeshPluginException(
					String.format("License insufficient. Plugin '%s' is not licensed",
							licensedPlugin.getProductName()));
		}
	}

}

