package com.gentics.mesh.core.data;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation for Version Numbers in the form [major].[minor]
 */
public class VersionNumber {
	private final static Pattern pattern = Pattern.compile("([0-9]+)\\.([0-9]+)");

	private int major;

	private int minor;

	private String fullVersion;

	/**
	 * Create an instance for Version 0.1
	 */
	public VersionNumber() {
		this(0, 1);
	}

	/**
	 * Create an instance with Version [major].[minor]
	 * @param major Major Number
	 * @param minor Minor Number
	 */
	public VersionNumber(int major, int minor) {
		if (major < 0) {
			throw new IllegalArgumentException("Major Version Number must not be negative, but was " + major);
		}
		if (minor < 0) {
			throw new IllegalArgumentException("Minor Version Number must not be negative, but was " + minor);
		}
		this.major = major;
		this.minor = minor;
		fullVersion = major + "." + minor;
	}

	/**
	 * Create a Version with given version
	 * @param fullVersion version must match the pattern [major].[minor]
	 */
	public VersionNumber(String fullVersion) {
		if (fullVersion == null) {
			throw new IllegalArgumentException("Version must not be null");
		}
		Matcher matcher = pattern.matcher(fullVersion);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Version " + fullVersion + " does not match the expected pattern [major].[minor]");
		}

		this.major = Integer.parseInt(matcher.group(1));
		this.minor = Integer.parseInt(matcher.group(2));
		this.fullVersion = fullVersion;
	}

	/**
	 * Return the next draft version number
	 * @return next draft version number
	 */
	public VersionNumber nextDraft() {
		return new VersionNumber(major, minor + 1);
	}

	/**
	 * Return the next published version number
	 * @return next published version number
	 */
	public VersionNumber nextPublished() {
		return new VersionNumber(major + 1, 0);
	}

	@Override
	public String toString() {
		return fullVersion;
	}
}
