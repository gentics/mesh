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
	 * Check whether the request is a read request.
	 * 
	 * @param method
	 * @param path
	 * @return
	 */
	public static boolean isReadRequest(HttpMethod method, String path) {
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
}
