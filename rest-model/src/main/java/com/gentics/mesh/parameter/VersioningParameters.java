package com.gentics.mesh.parameter;

public interface VersioningParameters extends ParameterProvider {

	public static final String RELEASE_QUERY_PARAM_KEY = "release";

	public static final String VERSION_QUERY_PARAM_KEY = "version";

	/**
	 * Return the currently configured version.
	 * 
	 * @return
	 */
	String getVersion();

	/**
	 * Return the currently configured release name.
	 * 
	 * @return
	 */
	String getRelease();

	/**
	 * Set the release by name or UUID.
	 *
	 * @param release
	 *            name or uuid
	 * @return fluent API
	 */
	VersioningParameters setRelease(String release);

	/**
	 * Set the version. This can be either "draft", "published" or a version number
	 *
	 * @param version
	 *            version
	 * @return fluent API
	 */
	VersioningParameters setVersion(String version);

	/**
	 * Request the draft version. Alias for setVersion("draft")
	 * 
	 * @return fluent API
	 */
	VersioningParameters draft();

	/**
	 * Request the published version. Alias for setVersion("published")
	 * 
	 * @return fluent API
	 */
	VersioningParameters published();

}
