package com.gentics.cailun.core.rest.request;

import com.gentics.cailun.core.rest.response.RestUserResponse;

public class RestUserUpdateRequest extends RestUserResponse {

	private String password;

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}
}
