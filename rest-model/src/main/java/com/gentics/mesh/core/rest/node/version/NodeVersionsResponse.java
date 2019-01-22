package com.gentics.mesh.core.rest.node.version;

import java.util.List;

import com.gentics.mesh.core.rest.common.RestModel;

public class NodeVersionsResponse implements RestModel {

	private List<VersionInfo> versions;

	public NodeVersionsResponse() {
	}

	public List<VersionInfo> getVersions() {
		return versions;
	}

	public void setVersions(List<VersionInfo> versions) {
		this.versions = versions;
	}
}
