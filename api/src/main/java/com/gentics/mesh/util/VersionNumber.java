package com.gentics.mesh.util;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation for Version Numbers in the form [major].[minor]
 */
public class VersionNumber implements Comparable<VersionNumber> {

	private final static Pattern pattern = Pattern.compile("([0-9]+)\\.([0-9]+)");

	private final int major;

	private final int minor;

	private final String fullVersion;

	/**
	 * Create an instance for Version 0.1
	 */
	public VersionNumber() {
		this(0, 1);
	}

	/**
	 * Create an instance with Version [major].[minor]
	 * 
	 * @param major
	 *            Major Number
	 * @param minor
	 *            Minor Number
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
	 * 
	 * @param fullVersion
	 *            version must match the pattern [major].[minor]
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
	 * 
	 * @return next draft version number
	 */
	public VersionNumber nextDraft() {
		return new VersionNumber(major, minor + 1);
	}

	/**
	 * Return the next published version number
	 * 
	 * @return next published version number
	 */
	public VersionNumber nextPublished() {
		return new VersionNumber(major + 1, 0);
	}

	/**
	 * Return the full version string.
	 */
	@Override
	public String toString() {
		return getFullVersion();
	}

	/**
	 * Return the full version string.
	 * 
	 * @return
	 */
	public String getFullVersion() {
		return fullVersion;
	}

	/**
	 * Compare the versions.
	 * 
	 * @param versionNumber
	 *            Version number to compare against
	 * @return The result is a negative integer if versionNumber is _numerically_ less than this number. The result is a positive integer if versionNumber is
	 *         _numerically_ greater than this number. The result is zero if the version numbers are _numerically_ equal.
	 */
	@Override
	public int compareTo(VersionNumber versionNumber) {
		return versionCompare(toString(), versionNumber.toString());
	}

	/**
	 * Compare the versions.
	 * 
	 * @param versionNumber
	 *            Version number string to compare against
	 * @return The result is a negative integer if o is _numerically_ less than this number. The result is a positive integer if o is _numerically_ greater than
	 *         this number. The result is zero if the version numbers are _numerically_ equal.
	 */
	public int compareTo(String versionNumber) {
		return versionCompare(toString(), versionNumber);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (this.toString().equals(obj.toString())) {
			return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(major, minor);
	}

	/**
	 * Compares two version strings.
	 * 
	 * Use this instead of String.compareTo() for a non-lexicographical comparison that works for version strings. e.g. "1.10".compareTo("1.6").
	 * 
	 * It does not work if "1.10" is supposed to be equal to "1.10.0".
	 * 
	 * @param str1
	 *            a string of ordinal numbers separated by decimal points.
	 * @param str2
	 *            a string of ordinal numbers separated by decimal points.
	 * @return The result is a negative integer if str1 is _numerically_ less than str2. The result is a positive integer if str1 is _numerically_ greater than
	 *         str2. The result is zero if the strings are _numerically_ equal.
	 */
	public static int versionCompare(String str1, String str2) {
		String[] vals1 = str1.split("\\.");
		String[] vals2 = str2.split("\\.");
		int i = 0;
		// set index to first non-equal ordinal or length of shortest version string
		while (i < vals1.length && i < vals2.length && vals1[i].equals(vals2[i])) {
			i++;
		}
		// compare first non-equal ordinal number
		if (i < vals1.length && i < vals2.length) {
			int diff = Integer.valueOf(vals1[i]).compareTo(Integer.valueOf(vals2[i]));
			return Integer.signum(diff);
		}
		// the strings are equal or one string is a substring of the other
		// e.g. "1.2.3" = "1.2.3" or "1.2.3" < "1.2.3.4"
		return Integer.signum(vals1.length - vals2.length);
	}

	/**
	 * Get the major version part
	 * @return
	 */
	public int getMajor() {
		return major;
	}

	/**
	 * Get the minor version part
	 * @return
	 */
	public int getMinor() {
		return minor;
	}
}
