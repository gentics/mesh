package com.gentics.mesh.core.rest.user;

import com.gentics.mesh.core.rest.common.AbstractRestModel;

public class UserUpdateRequest extends AbstractRestModel {

	private String password;

	private String lastname;

	private String firstname;

	private String username;

	private String emailAddress;

	public UserUpdateRequest() {
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

}
