package com.gentics.mesh.rest;

public abstract class AbstractAuthentication implements MeshRestClientAuthentication {

	private String username;
	private String password;
	
	@Override
	public void setLogin(String username, String password) {
		this.username = username;
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
