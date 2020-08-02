package com.gentics.mesh.parameter;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.util.VersionNumber;

public interface VersioningParameters extends ParameterProvider {

	public static final String BRANCH_QUERY_PARAM_KEY = "branch";

	public static final String VERSION_QUERY_PARAM_KEY = "version";

	/**
	 * Return the currently configured version.
	 * 
	 * @return
	 */
	default String getVersion() {
		String version = "draft";
		String versionParameter = getParameter(VERSION_QUERY_PARAM_KEY);
		if (versionParameter != null) {
			if ("draft".equalsIgnoreCase(versionParameter) || "published".equalsIgnoreCase(versionParameter)) {
				version = versionParameter;
			} else {
				try {
					version = new VersionNumber(versionParameter).toString();
				} catch (IllegalArgumentException e) {
					throw error(BAD_REQUEST, "error_illegal_version", versionParameter);
				}
			}
		}
		return version;
	}

	/**
	 * Check whether a version was specified in the parameters.
	 * 
	 * @return
	 */
	default boolean hasVersion() {
		return getParameter(VERSION_QUERY_PARAM_KEY) != null;
	}

	/**
	 * Set the version. This can be either "draft", "published" or a version number
	 *
	 * @param version
	 *            version
	 * @return fluent API
	 */
	default VersioningParameters setVersion(String version) {
		setParameter(VERSION_QUERY_PARAM_KEY, version);
		return this;
	}

	/**
	 * Return the currently configured branch name.
	 * 
	 * @return
	 */
	default String getBranch() {
		return getParameter(BRANCH_QUERY_PARAM_KEY);
	}

	/**
	 * Set the branch by name or UUID.
	 *
	 * @param branch
	 *            name or uuid
	 * @return fluent API
	 */
	default VersioningParameters setBranch(String branch) {
		setParameter(BRANCH_QUERY_PARAM_KEY, branch);
		return this;
	}

	/**
	 * Request the draft version. Alias for setVersion("draft")
	 * 
	 * @return fluent API
	 */
	default VersioningParameters draft() {
		return setVersion("draft");
	}

	/**
	 * Request the published version. Alias for setVersion("published")
	 * 
	 * @return fluent API
	 */
	default VersioningParameters published() {
		return setVersion("published");
	}

}
