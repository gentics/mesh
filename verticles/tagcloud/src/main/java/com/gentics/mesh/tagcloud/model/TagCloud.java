package com.gentics.mesh.tagcloud.model;

import java.util.HashSet;
import java.util.Set;

public class TagCloud {

	public Set<TagCloudEntry> entries = new HashSet<>();

	public Set<TagCloudEntry> getEntries() {
		return entries;
	}

}
