package com.gentics.mesh.core.rest.auth;

import com.gentics.mesh.core.rest.common.RestModel;

public class LoginRequest implements RestModel {

	private String username;
	private String password;

	public String getPassword() {
		return password;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

}
