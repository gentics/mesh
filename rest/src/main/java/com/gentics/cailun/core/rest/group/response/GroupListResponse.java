package com.gentics.cailun.core.rest.group.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.cailun.core.rest.common.response.AbstractRestListResponse;

public class GroupListResponse extends AbstractRestListResponse {

	List<GroupResponse> groups = new ArrayList<>();

	public GroupListResponse() {
	}

	public void addGroup(GroupResponse group) {
		this.groups.add(group);
	}

	public List<GroupResponse> getGroups() {
		return groups;
	}

}
