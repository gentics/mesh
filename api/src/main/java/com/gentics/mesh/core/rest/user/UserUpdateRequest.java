package com.gentics.mesh.core.rest.user;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a user update request.
 */
public class UserUpdateRequest implements RestModel {

	private String password;

	private String lastname;

	private String firstname;

	private String username;

	private String emailAddress;

	private NodeReference nodeReference;

	public UserUpdateRequest() {
	}

	/**
	 * Set the user plain text password. The password will be hashed when the request is processed.
	 * 
	 * @param password
	 *            Plain text password
	 * @return Fluent API
	 */
	public UserUpdateRequest setPassword(String password) {
		this.password = password;
		return this;
	}

	/**
	 * Return the plain text password.
	 * 
	 * @return plain text password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Return the user lastname.
	 * 
	 * @return Lastname of the user
	 */
	public String getLastname() {
		return lastname;
	}

	/**
	 * Set the user lastname.
	 * 
	 * @param lastname
	 *            Lastname of the user
	 * @return Fluent API
	 */
	public UserUpdateRequest setLastname(String lastname) {
		this.lastname = lastname;
		return this;
	}

	/**
	 * Return the user firstname.
	 * 
	 * @return Firstname of the user
	 */
	public String getFirstname() {
		return firstname;
	}

	/**
	 * Set the user firstname.
	 * 
	 * @param firstname
	 *            Firstname of the user
	 * @return Fluent API
	 */
	public UserUpdateRequest setFirstname(String firstname) {
		this.firstname = firstname;
		return this;
	}

	/**
	 * Return the user email address.
	 * 
	 * @return Email address of the user
	 */
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * Set the user email address.
	 * 
	 * @param emailAddress
	 * @return Fluent API
	 */
	public UserUpdateRequest setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
		return this;
	}

	/**
	 * Return the username.
	 * 
	 * @return Username of the user
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the username.
	 * 
	 * @param username
	 *            Username of the user
	 * @return Fluent API
	 */
	public UserUpdateRequest setUsername(String username) {
		this.username = username;
		return this;
	}

	/**
	 * Return the user reference node.
	 * 
	 * @return Node reference of the user
	 */
	public NodeReference getNodeReference() {
		return nodeReference;
	}

	/**
	 * Set the user reference node which can be used to store additional user specific data.
	 * 
	 * @param nodeReference
	 *            Node reference of the user
	 * @return Fluent API
	 */
	public UserUpdateRequest setNodeReference(NodeReference nodeReference) {
		this.nodeReference = nodeReference;
		return this;
	}

}
