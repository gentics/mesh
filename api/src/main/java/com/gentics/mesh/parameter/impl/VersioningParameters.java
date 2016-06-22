package com.gentics.mesh.parameter.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.VersionNumber;

public class VersioningParameters extends AbstractParameters {

	public static final String RELEASE_QUERY_PARAM_KEY = "release";

	public static final String VERSION_QUERY_PARAM_KEY = "version";

	public VersioningParameters(ActionContext ac) {
		super(ac);
	}

	public VersioningParameters() {
	}

	/**
	 * Set the release by name or UUID.
	 *
	 * @param release
	 *            name or uuid
	 * @return fluent API
	 */
	public VersioningParameters setRelease(String release) {
		setParameter(RELEASE_QUERY_PARAM_KEY, release);
		return this;
	}

	public String getVersion() {
		String version = "published";
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
	 * Set the version. This can be either "draft", "published" or a version number
	 *
	 * @param version
	 *            version
	 * @return fluent API
	 */

	public VersioningParameters setVersion(String version) {
		setParameter(VERSION_QUERY_PARAM_KEY, version);
		return this;
	}

	/**
	 * Request the draft version.
	 * 
	 * @return fluent API
	 */
	public VersioningParameters draft() {
		return setVersion("draft");
	}

	public String getRelease() {
		return getParameter(RELEASE_QUERY_PARAM_KEY);
	}

}
