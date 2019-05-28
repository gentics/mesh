package com.gentics.mesh.core.rest.node.version;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Response POJO which is used to return version lists of a node.
 */
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

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (String key : getVersions().keySet()) {
			builder.append(key + " : " + listVersions(key) + "\n");
		}
		return builder.toString();
	}

	/**
	 * Returns a human readable version list for the given language.
	 * 
	 * @param languageTag
	 * @return
	 */
	public String listVersions(String languageTag) {

		StringBuilder builder = new StringBuilder();
		List<VersionInfo> list = getVersions().get(languageTag);
		if (list == null) {
			return "";
		}
		Iterator<VersionInfo> it = list.iterator();
		while (it.hasNext()) {
			VersionInfo v = it.next();
			if (v.getPublished()) {
				builder.append("P");
			}
			if (v.getDraft()) {
				builder.append("D");
			}
			if (v.getBranchRoot()) {
				builder.append("I");
			}
			builder.append("(" + v.getVersion() + ")");
			if (it.hasNext()) {
				builder.append("=>");
			}
		}
		return builder.toString();
	}
}