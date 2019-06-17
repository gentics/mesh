package com.gentics.mesh.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Implementation for Maven Version Numbers in the form [major].[minor].[bugfix][-SNAPSHOT]
 */
public class MavenVersionNumber implements Comparable<MavenVersionNumber> {

	/**
	 * Pattern for version numbers (without branch specifier)
	 */
	public static Pattern snapshotVersion = Pattern.compile("([0-9]+)\\.([0-9]+)\\.([0-9]+)-?(.*)?");

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
	protected boolean isSnapshot;

	/**
	 * Postfix in the version
	 */
	protected String postfix;

	/**
	 * Full version number
	 */
	protected String fullVersion;

	/**
	 * Create an empty instance
	 */
	protected MavenVersionNumber() {
	}

	/**
	 * Parse the given version number string into a version number object
	 * 
	 * @param versionNumberString
	 *            version number string
	 * @return version number object or null if not parseable
	 */
	public static MavenVersionNumber parse(String versionNumberString) {
		if (versionNumberString == null) {
			return null;
		}
		Matcher m = snapshotVersion.matcher(versionNumberString);

		if (!m.matches()) {
			return null;
		}

		MavenVersionNumber versionNumber = new MavenVersionNumber();

		versionNumber.fullVersion = versionNumberString;
		versionNumber.major = Integer.parseInt(m.group(1));
		versionNumber.minor = Integer.parseInt(m.group(2));
		versionNumber.patch = Integer.parseInt(m.group(3));
		String postfix = m.group(4);
		if (postfix != null) {
			postfix = postfix.replaceAll("-SNAPSHOT", "");
			versionNumber.postfix = postfix;
		}
		versionNumber.isSnapshot = versionNumberString.endsWith("-SNAPSHOT");
		return versionNumber;
	}

	/**
	 * Get 0 if a snapshot or 1 if not a snapshot
	 * 
	 * @return 0 for snapshot, 1 otherwise
	 */
	protected int getSnapshotValue() {
		return isSnapshot ? 0 : 1;
	}

	/**
	 * Get the version as [major].[minor] (without patchlevel)
	 * 
	 * @return version as [major].[minor]
	 */
	public String getMajorMinor() {
		return major + "." + minor;
	}

	@Override
	public String toString() {
		return fullVersion;
	}

	public int compareTo(MavenVersionNumber other) {
		return compareTo(other, true);
	}

	/**
	 * Compare both versions.
	 * 
	 * @param other
	 *            Version to be compared to
	 * @param Flag
	 *            which indicates whether the snapshot flag should be compared as well.
	 * 
	 * @return 0, if same versions. Greater 0 if other is older. Otherwise -1
	 */
	public int compareTo(MavenVersionNumber other, boolean checkSnapshot) {
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
		if (diff == 0 && checkSnapshot) {
			diff = getSnapshotValue() - other.getSnapshotValue();
		}
		return diff;
	}

	public String getPostfix() {
		return postfix;
	}

	/**
	 * Return the flag which indicates whether the parsed version is a snapshot version.
	 * 
	 * @return
	 */
	public boolean isSnapshot() {
		return isSnapshot;
	}

}
