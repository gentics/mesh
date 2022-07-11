package com.gentics.mesh.distributed;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Utility methods for distributed computing
 */
public class DistributionUtils {
	private static final Logger log = LoggerFactory.getLogger(DistributionUtils.class);

	private static final Set<Pattern> readOnlyPathPatternSet = createReadOnlyPatternSet();

	/**
	 * Set of black-listed path patterns (paths matching any of these patterns is supposed to be "modifying" and should be redirected to the master)
	 */
	private static final Set<Pattern> blackListPathPatternSet = createBlackListPatternSet();

	/**
	 * Check whether the request is a read request.
	 * 
	 * @param method
	 * @param path
	 * @return
	 */
	public static boolean isReadRequest(HttpMethod method, String path) {
		if (isBlackListed(path)) {
			return false;
		}

		switch (method) {
		case CONNECT:
		case OPTIONS:
		case GET:
			return true;
		case DELETE:
		case PATCH:
		case PUT:
			return false;
		case POST:
			// Lets check whether the request is actually a read request.
			// In this case we don't need to delegate it.
			return isReadOnly(path);
		default:
			log.debug("Unhandled methd {" + method + "} in path {" + path + "}");
			return false;
		}
	}

	/**
	 * Check whether the given path matches one of the known read-only paths.
	 * @param path path
	 * @return true if the request to the given path is considered read-only
	 */
	public static boolean isReadOnly(String path) {
		for (Pattern pattern : readOnlyPathPatternSet) {
			Matcher m = pattern.matcher(path);
			if (m.matches()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check whether the given path matches one of the known black-listed paths.
	 * @param path path
	 * @return true if the request to the given path is black-listed (is supposed to be "modifying")
	 */
	public static boolean isBlackListed(String path) {
		for (Pattern pattern : blackListPathPatternSet) {
			Matcher m = pattern.matcher(path);
			if (m.matches()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Create the set of read-only patterns
	 * @return pattern set
	 */
	private static Set<Pattern> createReadOnlyPatternSet() {
		Set<Pattern> patterns = new HashSet<>();
		patterns.add(Pattern.compile("/api/v[0-9]+/auth/login/?"));
		patterns.add(Pattern.compile("/api/v[0-9]+/.*/graphql/?"));
		patterns.add(Pattern.compile("/api/v[0-9]+/search/?"));
		patterns.add(Pattern.compile("/api/v[0-9]+/rawSearch/?"));
		patterns.add(Pattern.compile("/api/v[0-9]+/.*/search/?"));
		patterns.add(Pattern.compile("/api/v[0-9]+/.*/rawSearch/?"));
		patterns.add(Pattern.compile("/api/v[0-9]+/utilities/linkResolver/?"));
		patterns.add(Pattern.compile("/api/v[0-9]+/utilities/validateSchema/?"));
		patterns.add(Pattern.compile("/api/v[0-9]+/utilities/validateMicroschema/?"));
		return patterns;
	}

	/**
	 * Create the set of blacklisted path patterns
	 * @return pattern set
	 */
	private static Set<Pattern> createBlackListPatternSet() {
		Set<Pattern> patterns = new HashSet<>();
		// clearing the search indices should only be done on the Master
		patterns.add(Pattern.compile("/api/v[0-9]+/search/clear"));
		// index sync should only be done on the Master
		patterns.add(Pattern.compile("/api/v[0-9]+/search/sync"));
		// search index operation status should only be fetched from the Master (which is doing the index operations) 
		patterns.add(Pattern.compile("/api/v[0-9]+/search/status"));
		return patterns;
	}
}
