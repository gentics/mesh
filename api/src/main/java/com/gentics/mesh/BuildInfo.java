package com.gentics.mesh;

import java.util.Properties;

/**
 * POJO to map the properties from the mesh.build.properties resource file.
 * The file is updated via maven property filtering during build/release time.
 */
public class BuildInfo {

	private String version;
	private String buildtimestamp;

	public BuildInfo(String version, String buildtimestamp) {
		this.version = version;
		this.buildtimestamp = buildtimestamp;
	}

	/**
	 * Create a new build info using the provided properties.
	 * 
	 * @param buildProperties
	 */
	public BuildInfo(Properties buildProperties) {
		this(buildProperties.getProperty("mesh.version"), buildProperties.getProperty("mesh.build.timestamp"));
	}

	/**
	 * Return the build timestamp.
	 * 
	 * @return
	 */
	public String getBuildtimestamp() {
		return buildtimestamp;
	}

	/**
	 * Set the build timestamp
	 * 
	 * @param buildtimestamp
	 * @return Fluent API
	 */
	public BuildInfo setBuildtimestamp(String buildtimestamp) {
		this.buildtimestamp = buildtimestamp;
		return this;
	}

	/**
	 * Return the version string.
	 * 
	 * @return
	 */
	public String getVersion() {
		String overrideVersion = System.getProperty("mesh.internal.version");
		if (overrideVersion == null) {
			return version;
		} else {
			return overrideVersion;
		}

	}

	/**
	 * Set the version string.
	 * 
	 * @param version
	 * @return
	 */
	public BuildInfo setVersion(String version) {
		this.version = version;
		return this;
	}

	@Override
	public String toString() {
		return getVersion() + " " + getBuildtimestamp();
	}
}
