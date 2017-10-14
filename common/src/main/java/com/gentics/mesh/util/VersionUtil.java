package com.gentics.mesh.util;

public final class VersionUtil {

	private VersionUtil() {

	}

	/**
	 * Compare both versions and return the delta of the versions.
	 * 
	 * @param version1
	 * @param version2
	 * @return 0, when both versions are equal, &lt;0 when the second version is larger, &gt;0 when the second version is smaller
	 */
	public static int compareVersions(String version1, String version2) {

		String[] levels1 = version1.split("\\.");
		String[] levels2 = version2.split("\\.");

		int length = Math.max(levels1.length, levels2.length);
		for (int i = 0; i < length; i++) {
			Integer v1 = i < levels1.length ? Integer.parseInt(levels1[i]) : 0;
			Integer v2 = i < levels2.length ? Integer.parseInt(levels2[i]) : 0;
			int compare = v1.compareTo(v2);
			if (compare != 0) {
				return compare;
			}
		}

		return 0;
	}
}
