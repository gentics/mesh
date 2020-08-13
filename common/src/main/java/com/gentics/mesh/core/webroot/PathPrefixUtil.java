package com.gentics.mesh.core.webroot;

import com.gentics.mesh.core.data.branch.HibBranch;

/**
 * Utility class used to deal with path prefix.
 */
public final class PathPrefixUtil {

	private PathPrefixUtil() {

	}

	/**
	 * Sanitize the path prefix. This method will ensure that the path prefix has the correct format.
	 * 
	 * @param prefix
	 * @return
	 */
	public static String sanitize(String prefix) {
		if (prefix == null) {
			return "";
		}
		if (prefix.isEmpty()) {
			return prefix;
		}
		prefix = prefix.trim();
		// Ensure that the prefix starts with a slash
		if (!prefix.startsWith("/")) {
			prefix = "/" + prefix;
		}
		// Remove tailing slash
		if (prefix.endsWith("/")) {
			prefix = prefix.substring(0, prefix.length() - 1);
		}
		return prefix;
	}

	/**
	 * Remove the configured path prefix of the branch from the given path.
	 * 
	 * @param branch
	 *            Branch which contains the prefix
	 * @param path
	 *            Path to be handled
	 * @return stripped path
	 */
	public static String strip(HibBranch branch, String path) {
		String prefix = branch.getPathPrefix();
		prefix = sanitize(prefix);
		return !startsWithPrefix(branch, path) ? path : path.substring(prefix.length());
	}

	/**
	 * Check whether the given path starts with the path prefix of the branch.
	 * 
	 * @param branch
	 * @param path
	 * @return
	 */
	public static boolean startsWithPrefix(HibBranch branch, String path) {
		String prefix = branch.getPathPrefix();
		prefix = sanitize(prefix);
		return path.startsWith(prefix);
	}

}
