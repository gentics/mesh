package com.gentics.mesh.enterprise.service.license;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.license.License;
import com.gentics.license.LicenseChecker;
import com.gentics.license.LicenseStatus;
import com.gentics.license.feature.LicenseKeyFeature;
import com.gentics.mesh.etc.config.HibernateMeshOptions;

/**
 * Service to perform license checks. I.e. check which services are licensed and allowed to use.
 * This is a wrapper implementation around the LicenseChecker
 */
public class LicenseService {

	protected static final Logger logger = LoggerFactory.getLogger(LicenseService.class);

	private final HibernateMeshOptions options;

	public LicenseService(HibernateMeshOptions options) {
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
			logger.error("License check failed.", exception);
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
		boolean isLicensed = LicenseChecker.getLicenseCheckResult().containsFeature(LicensedFeature.MSQL);

		if (!isLicensed) {
			logger.warn("Feature {} database connector is not licensed", LicensedFeature.MSQL.getProductName());
		}
		return isLicensed;
	}

	/**
	 * Get license info.
	 * 
	 * @return
	 */
	public LicenseResponse getLicenseInfo() {
		License license = LicenseChecker.getLicense();
		if (license != null) {
			LicenseKeyFeature[] features = LicenseChecker.getLicenseCheckResult().getFeatures();
			LicenseStatus status = LicenseChecker.getLicenseCheckResult().getLicenseResult();

			LicenseResponse response = new LicenseResponse();
			response.setStatus(status.name());
			response.setFeatures(Arrays.stream(features).map(LicenseKeyFeature::getProductName).collect(Collectors.toSet()));
			{
				String[] parts = license.getLicense().split("-");
				String obfuscated = IntStream.range(0, parts.length).mapToObj(i -> {
					if (i > 0 && i < (parts.length-1)) {
						return "****";
					} else {
						return parts[i];
					}
				}).collect(Collectors.joining("-"));
				response.setLicenseKey(obfuscated);
			}

			return response;
		} else {
			return null;
		}
	}
}

