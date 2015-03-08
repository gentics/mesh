package com.gentics.cailun.core.rest.user.request;

import com.gentics.cailun.core.rest.user.response.UserResponse;

public class UserUpdateRequest extends UserResponse {

	private String password;

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}
}
