package com.gentics.cailun.core.rest.response;

import java.util.ArrayList;
import java.util.List;

public class RestGroupList extends AbstractRestListResponse {

	List<RestGroup> groups = new ArrayList<>();

	public RestGroupList() {
	}

	public void addGroup(RestGroup group) {
		this.groups.add(group);
	}

	public List<RestGroup> getGroups() {
		return groups;
	}

}
