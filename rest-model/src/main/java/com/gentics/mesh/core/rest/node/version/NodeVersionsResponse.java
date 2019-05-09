package com.gentics.mesh.core.rest.node.version;

import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.rest.common.RestModel;

public class NodeVersionsResponse implements RestModel {

	private Map<String, List<VersionInfo>> versions;

	public NodeVersionsResponse() {
	}

	public Map<String, List<VersionInfo>> getVersions() {
		return versions;
	}

	public void setVersions(Map<String, List<VersionInfo>> versions) {
		this.versions = versions;
	}
}