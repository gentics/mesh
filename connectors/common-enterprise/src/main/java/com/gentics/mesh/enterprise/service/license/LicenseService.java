package com.gentics.mesh.enterprise.service.license;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gentics.license.License;
import com.gentics.license.LicenseCheckResult;
import com.gentics.license.LicenseChecker;
import com.gentics.license.LicenseStatus;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.util.UUIDUtil;

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
			LicenseChecker.init(List.of(LicensedFeature.MSQL.name()), licenseKeyFile);

			LicenseChecker.checkLicense();
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
		LicenseCheckResult checkResult = LicenseChecker.getLicenseCheckResult();
		LicenseResponse response = new LicenseResponse();
		LicenseStatus status = checkResult.getLicenseResult();
		response.setStatus(status.name());
		License license = checkResult.getLicense();
		if (license != null) {
			Collection<String> features = checkResult.getFeatures();
			response.setIssuedAt(license.getIssuedAt().toInstant());
			response.setExpiresAt(license.getValidUntil().toInstant());
			response.setIssuer(license.getIssuer());
			response.setSubject(license.getSubject());
			response.setFeatures(new HashSet<>(features));
			String uuid = UUIDUtil.toShortUuid(license.getUuid());
			response.setLicenseKey(IntStream.range(0, uuid.length()).mapToObj(i -> {
				if (i > 2 && i < (uuid.length() - 2)) {
					return '*';
				} else {
					return uuid.charAt(i);
				}
			}).map(Object::toString).collect(Collectors.joining()));
		}
		return response;
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

