package com.gentics.mesh.enterprise.service.license;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.license.License;
import com.gentics.license.LicenseChecker;
import com.gentics.license.LicenseStatus;
import com.gentics.license.feature.LicenseKeyFeature;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.github.jknack.handlebars.internal.lang3.StringUtils;

/**
 * Service to perform license checks. I.e. check which services are licensed and allowed to use.
 * This is a wrapper implementation around the LicenseChecker
 */
public class LicenseService {

	protected static final Logger logger = LoggerFactory.getLogger(LicenseService.class);

	private final HibernateMeshOptions options;

	public LicenseService(HibernateMeshOptions options) {
		this.options = options;
		reset();
	}

	/**
	 * Re/Initialize the LicenseChecker. Reads the license.key file and register available features.
	 *
	 * @return true if initializing was successful (e.g. valid license.key file)
	 */
	public LicenseStatus reset() {
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
			return LicenseStatus.INVALID;
		}
		return LicenseChecker.getLicenseCheckResult().getLicenseResult();
	}

	/**
	 * Checks if the feature has a valid license
	 *
	 * @return true if license is sufficient
	 */
	public boolean isLicenseSufficient() {
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
			if (StringUtils.isNotBlank(license.getLicense())) {
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

	/**
	 * Get the license key file, if exists.
	 * 
	 * @return
	 */
	public Optional<File> getLicenseFile() {
		return Optional.ofNullable(Files.exists(Path.of(options.getLicenseKeyPath()))).filter(Boolean::booleanValue).map(exists -> new File(options.getLicenseKeyPath()));
	}
}

