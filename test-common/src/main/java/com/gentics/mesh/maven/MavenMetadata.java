package com.gentics.mesh.maven;

import java.util.List;

/**
 * POJO which represents the contents of a maven-metadata.xml
 */
public class MavenMetadata {

	private String groupId;
	private String artifactId;
	private String version;

	private int buildNumber;
	private String timestamp;
	private boolean snapshot;
	private List<String> versions;

	/**
	 * Creates a new metadata descriptor. By default the descriptor is not describing a snapshot artifact.
	 * 
	 * @param groupId
	 * @param artifactId
	 * @param version
	 */
	public MavenMetadata(String groupId, String artifactId, String version) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
		this.snapshot = false;
	}

	/**
	 * Returns the groupId of this metadata descriptor
	 * 
	 * @return
	 */
	public String getGroupId() {
		return groupId;
	}

	/**
	 * Sets the groupId of this metadata descriptor
	 * 
	 * @param groupId
	 */
	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	/**
	 * Gets the artifact id of this metadata descriptor.
	 * 
	 * @return
	 */
	public String getArtifactId() {
		return artifactId;
	}

	/**
	 * Sets the artifactId.
	 * 
	 * @param artifactId
	 */
	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	/**
	 * Returns the version of this artifact.
	 * 
	 * @return
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Set the artifact version.
	 * 
	 * @param version
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Return the build number of the latest artifact.
	 * 
	 * @return
	 */
	public int getBuildNumber() {
		return buildNumber;
	}

	/**
	 * Set the build number for the lastest artifact.
	 * 
	 * @param buildNumber
	 */
	public void setBuildNumber(int buildNumber) {
		this.buildNumber = buildNumber;
	}

	/**
	 * Set the update timestamp.
	 * 
	 * @param timestamp
	 */
	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Return the update timestamp.
	 * 
	 * @return
	 */
	public String getTimestamp() {
		return timestamp;
	}

	/**
	 * Returns whether this artifact is an snapshot artifact or not.
	 * 
	 * @return
	 */
	public boolean isSnapshot() {
		return snapshot;
	}

	/**
	 * Flag this artifact as an snapshot artifact.
	 * 
	 * @param snapshot
	 */
	public void setSnapshot(boolean snapshot) {
		this.snapshot = snapshot;
	}

	/**
	 * Return the list of versions.
	 * 
	 * @return
	 */
	public List<String> getVersions() {
		return versions;
	}

	/**
	 * Set the list of versions.
	 * 
	 * @param versions
	 */
	public void setVersions(List<String> versions) {
		this.versions = versions;
	}

}
