package com.gentics.cailun.core.rest.response;

import java.util.ArrayList;
import java.util.List;

public class RestUserList extends AbstractRestListResponse {

	private List<RestUserResponse> users = new ArrayList<>();

	public RestUserList() {
	}

	public void addUser(RestUserResponse user) {
		this.users.add(user);
	}

	public List<RestUserResponse> getUsers() {
		return users;
	}
}
