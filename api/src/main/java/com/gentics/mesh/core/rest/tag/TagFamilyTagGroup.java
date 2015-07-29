package com.gentics.mesh.core.rest.tag;

import java.util.ArrayList;
import java.util.List;

public class TagFamilyTagGroup {

	private String uuid;

	private List<TagReference> items = new ArrayList<>();

	public TagFamilyTagGroup() {
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	public List<TagReference> getItems() {
		return items;
	}
}
