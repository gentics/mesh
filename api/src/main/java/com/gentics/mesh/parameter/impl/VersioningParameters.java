package com.gentics.mesh.parameter.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.HashMap;
import java.util.Map;

import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.VersionNumber;

public class VersioningParameters extends AbstractParameters {

	public static final String RELEASE_QUERY_PARAM_KEY = "release";

	public static final String VERSION_QUERY_PARAM_KEY = "version";

	private String version;
	private String release;

	public VersioningParameters(ActionContext ac) {
		super(ac);
	}

	public VersioningParameters() {
	}

	@Override
	protected void constructFrom(ActionContext ac) {

		// version parameter
		String versionParameter = ac.getParameter(VERSION_QUERY_PARAM_KEY);
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
		} else {
			version = "published";
		}

		// release parameter
		release = ac.getParameter(RELEASE_QUERY_PARAM_KEY);

	}

	/**
	 * Set the release by name or UUID.
	 *
	 * @param release
	 *            name or uuid
	 * @return fluent API
	 */
	public VersioningParameters setRelease(String release) {
		this.release = release;
		return this;
	}

	public String getVersion() {
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
		this.version = version;
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
		return release;
	}

	@Override
	protected Map<String, Object> getParameters() {
		Map<String, Object> map = new HashMap<>();
		map.put(RELEASE_QUERY_PARAM_KEY, release);
		map.put(VERSION_QUERY_PARAM_KEY, version);
		return map;
	}
}
