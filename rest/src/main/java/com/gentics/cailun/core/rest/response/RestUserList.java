package com.gentics.cailun.core.rest.response;

import java.util.ArrayList;
import java.util.List;

public class RestUserList {

	private List<RestUser> users = new ArrayList<>();

	public RestUserList() {
	}

	public void addUser(RestUser user) {
		this.users.add(user);
	}

	public List<RestUser> getUsers() {
		return users;
	}
}
