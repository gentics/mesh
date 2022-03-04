package com.gentics.mesh.etc.config;

import java.io.File;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Options for disk quota check
 */
@GenerateDocumentation
public class DiskQuotaOptions implements Option {
	public final static Pattern QUOTA_PATTERN_PERCENTAGE = Pattern.compile("(?<value>[0-9]{1,2})%");

	public final static Pattern QUOTA_PATTERN_ABSOLUTE = Pattern.compile("(?<value>[0-9]+)(?<unit>b|B|k|K|m|M|g|G|t|T)");

	public final static int DEFAULT_CHECK_INTERVAL = 10_000;
	public final static String DEFAULT_WARN_THRESHOLD = "10%";
	public final static String DEFAULT_READ_ONLY_THRESHOLD = "2%";

	public final static String MESH_STORAGE_DISK_QUOTA_CHECK_INTERVAL_ENV = "MESH_STORAGE_DISK_QUOTA_CHECK_INTERVAL";
	public final static String MESH_STORAGE_DISK_QUOTA_WARN_THRESHOLD_ENV = "MESH_STORAGE_DISK_QUOTA_WARN_THRESHOLD";
	public final static String MESH_STORAGE_DISK_QUOTA_READ_ONLY_THRESHOLD_ENV = "MESH_STORAGE_DISK_QUOTA_READ_ONLY_THRESHOLD";

	@JsonProperty(defaultValue = DEFAULT_CHECK_INTERVAL + " ms")
	@JsonPropertyDescription("Check interval in ms. Setting this to 0 will disable the disk quota check. Default: " + DEFAULT_CHECK_INTERVAL)
	@EnvironmentVariable(name = MESH_STORAGE_DISK_QUOTA_CHECK_INTERVAL_ENV, description = "Overwrite the disk quota check interval.")
	private int checkInterval = DEFAULT_CHECK_INTERVAL;

	@JsonProperty(defaultValue = DEFAULT_WARN_THRESHOLD)
	@JsonPropertyDescription("Threshold for the disk quota warn level. This can be set either as percentage (e.g. 15%) or as absolute disk space (e.g. 10G). If less than the defined disk space is available, warnings will be logged. Default: " + DEFAULT_WARN_THRESHOLD)
	@EnvironmentVariable(name = MESH_STORAGE_DISK_QUOTA_WARN_THRESHOLD_ENV, description = "Overwrite the disk quota warn threshold.")
	private String warnThreshold = DEFAULT_WARN_THRESHOLD;

	private int warnThresholdPercent = -1;
	private long warnThresholdAbsolute = -1;

	@JsonProperty(defaultValue = DEFAULT_READ_ONLY_THRESHOLD)
	@JsonPropertyDescription("Threshold for the disk quota ready only level. This can be set either as percentage (e.g. 10%) or as absolute disk space (e.g. 5G). If less than the defined disk space is available, Mesh will automatically be set to readonly. Default: " + DEFAULT_READ_ONLY_THRESHOLD)
	@EnvironmentVariable(name = MESH_STORAGE_DISK_QUOTA_READ_ONLY_THRESHOLD_ENV, description = "Overwrite the disk quota ready only threshold.")
	private String readOnlyThreshold = DEFAULT_READ_ONLY_THRESHOLD;

	private int readOnlyThresholdPercent = -1;
	private long readOnlyThresholdAbsolute = -1;

	/**
	 * Check interval in ms
	 * @return check interval
	 */
	public int getCheckInterval() {
		return checkInterval;
	}

	/**
	 * Set the check interval in ms
	 * @param checkInterval in ms
	 * @return fluent API
	 */
	public DiskQuotaOptions setCheckInterval(int checkInterval) {
		this.checkInterval = checkInterval;
		return this;
	}

	/**
	 * Warn threshold
	 * @return threshold
	 */
	public String getWarnThreshold() {
		return warnThreshold;
	}

	/**
	 * Set the warn threshold
	 * @param warnThreshold warn threshold
	 * @return fluent API
	 */
	public DiskQuotaOptions setWarnThreshold(String warnThreshold) {
		this.warnThreshold = warnThreshold;
		return this;
	}

	/**
	 * Read only threshold
	 * @return threshold
	 */
	public String getReadOnlyThreshold() {
		return readOnlyThreshold;
	}

	/**
	 * Set the read only threshold
	 * @param readOnlyThreshold threshold
	 * @return fluent API
	 */
	public DiskQuotaOptions setReadOnlyThreshold(String readOnlyThreshold) {
		this.readOnlyThreshold = readOnlyThreshold;
		return this;
	}

	@JsonIgnore
	public long getAbsoluteWarnThreshold(File storageDir) {
		if (warnThresholdPercent < 0 || warnThresholdAbsolute < 0) {
			Matcher percentageMatcher = DiskQuotaOptions.QUOTA_PATTERN_PERCENTAGE.matcher(this.warnThreshold);
			Matcher absoluteMatcher = DiskQuotaOptions.QUOTA_PATTERN_ABSOLUTE.matcher(this.warnThreshold);

			if (percentageMatcher.matches()) {
				this.warnThresholdPercent = Integer.parseInt(percentageMatcher.group("value"));
				this.warnThresholdAbsolute = 0;
			} else if (absoluteMatcher.matches()) {
				this.warnThresholdPercent = 0;
				this.warnThresholdAbsolute = getBytes(absoluteMatcher);
			} else {
				this.warnThresholdPercent = 0;
				this.warnThresholdAbsolute = 0;
			}
		}
		if (warnThresholdPercent > 0) {
			return storageDir.getTotalSpace() * warnThresholdPercent / 100;
		} else if (warnThresholdAbsolute > 0) {
			return warnThresholdAbsolute;
		} else {
			return 0;
		}
	}

	@JsonIgnore
	public long getAbsoluteReadOnlyThreshold(File storageDir) {
		if (readOnlyThresholdPercent < 0 || readOnlyThresholdAbsolute < 0) {
			Matcher percentageMatcher = DiskQuotaOptions.QUOTA_PATTERN_PERCENTAGE.matcher(this.readOnlyThreshold);
			Matcher absoluteMatcher = DiskQuotaOptions.QUOTA_PATTERN_ABSOLUTE.matcher(this.readOnlyThreshold);

			if (percentageMatcher.matches()) {
				this.readOnlyThresholdPercent = Integer.parseInt(percentageMatcher.group("value"));
				this.readOnlyThresholdAbsolute = 0;
			} else if (absoluteMatcher.matches()) {
				this.readOnlyThresholdPercent = 0;
				this.readOnlyThresholdAbsolute = getBytes(absoluteMatcher);
			} else {
				this.readOnlyThresholdPercent = 0;
				this.warnThresholdAbsolute = 0;
			}
		}
		if (readOnlyThresholdPercent > 0) {
			return storageDir.getTotalSpace() * readOnlyThresholdPercent / 100;
		} else if (readOnlyThresholdAbsolute > 0) {
			return readOnlyThresholdAbsolute;
		} else {
			return 0;
		}
	}

	@Override
	public void validate(MeshOptions options) {
		if (checkInterval > 0) {
			Objects.requireNonNull(readOnlyThreshold, "The readOnlyThreshold must be specified.");
			if (!DiskQuotaOptions.QUOTA_PATTERN_ABSOLUTE.matcher(readOnlyThreshold).matches()
					&& !DiskQuotaOptions.QUOTA_PATTERN_PERCENTAGE.matcher(readOnlyThreshold).matches()) {
				throw new IllegalArgumentException("The readOnlyThreshold was set to " + readOnlyThreshold
						+ ", but is expected to either be a percentage (e.g. 10%) or an absolute memory size (like 10M)");
			}

			Objects.requireNonNull(warnThreshold, "The warnThreshold must be specified.");
			if (!DiskQuotaOptions.QUOTA_PATTERN_ABSOLUTE.matcher(warnThreshold).matches()
					&& !DiskQuotaOptions.QUOTA_PATTERN_PERCENTAGE.matcher(warnThreshold).matches()) {
				throw new IllegalArgumentException("The warnThreshold was set to " + warnThreshold
						+ ", but is expected to either be a percentage (e.g. 10%) or an absolute memory size (like 10M)");
			}
		}
	}

	private long getBytes(Matcher absoluteMatcher) {
		switch (absoluteMatcher.group("unit")) {
		case "b":
		case "B":
			return Long.parseLong(absoluteMatcher.group("value"));
		case "k":
		case "K":
			return Long.parseLong(absoluteMatcher.group("value")) * 1024;
		case "m":
		case "M":
			return Long.parseLong(absoluteMatcher.group("value")) * 1024 * 1024;
		case "g":
		case "G":
			return Long.parseLong(absoluteMatcher.group("value")) * 1024 * 1024 * 1024;
		case "t":
		case "T":
			return Long.parseLong(absoluteMatcher.group("value")) * 1024 * 1024 * 1024 * 1024;
		default:
			return 0;
		}
	}
}
