package com.gentics.mesh.rest;

public abstract class AbstractAuthenticationProvider implements MeshRestClientAuthenticationProvider {

	private String username;
	private String password;
	private String newPassword;

	@Override
	public void setLogin(String username, String password) {
		this.username = username;
		this.password = password;
		this.newPassword = null;
	}

	@Override
	public void setLogin(String username, String password, String newPassword) {
		setLogin(username, password);
		this.newPassword = newPassword;
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

	public String getNewPassword() {
		return newPassword;
	}

	public AbstractAuthenticationProvider setNewPassword(String newPassword) {
		this.newPassword = newPassword;
		return this;
	}
}
