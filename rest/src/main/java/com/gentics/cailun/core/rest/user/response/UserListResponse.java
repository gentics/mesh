package com.gentics.cailun.core.rest.user.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.cailun.core.rest.common.response.AbstractRestListResponse;

public class UserListResponse extends AbstractRestListResponse {

	private List<UserResponse> users = new ArrayList<>();

	public UserListResponse() {
	}

	public void addUser(UserResponse user) {
		this.users.add(user);
	}

	public List<UserResponse> getUsers() {
		return users;
	}
}
