package com.gentics.mesh.maven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comparable abstraction of a version number that has the format: (x.y.z-SNAPSHOT / x.y.z)
 */
public class VersionNumber implements Comparable<VersionNumber> {

	/**
	 * Pattern for version numbers (without branch specifier)
	 */
	public static final Pattern SNAPSHOT_VERSION = Pattern.compile("([0-9]+)\\.([0-9]+)\\.([0-9]+)(\\-SNAPSHOT)?");

	/**
	 * Major number
	 */
	protected int major;

	/**
	 * Minor number
	 */
	protected int minor;

	/**
	 * Patch level
	 */
	protected int patch;

	/**
	 * True for snapshots
	 */
	protected boolean snapshot;

	/**
	 * Full version number
	 */
	protected String fullVersion;

	/**
	 * Create an empty instance
	 */
	protected VersionNumber() {
	}

	/**
	 * Parse the given version number string into a version number object
	 * 
	 * @param versionNumberString
	 *            version number string
	 * @return version number object or null if not parseable
	 */
	public static VersionNumber parse(String versionNumberString) {
		if (versionNumberString == null) {
			return null;
		}
		Matcher m = SNAPSHOT_VERSION.matcher(versionNumberString);

		if (!m.matches()) {
			return null;
		}

		VersionNumber versionNumber = new VersionNumber();

		versionNumber.fullVersion = versionNumberString;
		versionNumber.major = Integer.parseInt(m.group(1));
		versionNumber.minor = Integer.parseInt(m.group(2));
		versionNumber.patch = Integer.parseInt(m.group(3));
		versionNumber.snapshot = m.groupCount() >= 4;
		return versionNumber;
	}

	/**
	 * Get 0 if a snapshot or 1 if not a snapshot
	 * 
	 * @return 0 for snapshot, 1 otherwise
	 */
	protected int getSnapshotValue() {
		return snapshot ? 0 : 1;
	}

	/**
	 * Get the version as [major].[minor] (without patchlevel)
	 * 
	 * @return version as [major].[minor]
	 */
	protected String getMajorMinor() {
		return major + "." + minor;
	}

	@Override
	public String toString() {
		return fullVersion;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	public int compareTo(VersionNumber other) {
		if (other == null) {
			return 1;
		}
		int diff = major - other.major;

		if (diff == 0) {
			diff = minor - other.minor;
		}
		if (diff == 0) {
			diff = patch - other.patch;
		}
		if (diff == 0) {
			diff = getSnapshotValue() - other.getSnapshotValue();
		}
		return diff;
	}
}
