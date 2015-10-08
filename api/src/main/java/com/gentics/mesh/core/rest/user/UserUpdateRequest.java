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
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Return the plain text password.
	 * 
	 * @return
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Return the user lastname.
	 * 
	 * @return
	 */
	public String getLastname() {
		return lastname;
	}

	/**
	 * Set the user lastname.
	 * 
	 * @param lastname
	 */
	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	/**
	 * Return the user firstname.
	 * 
	 * @return
	 */
	public String getFirstname() {
		return firstname;
	}

	/**
	 * Set the user firstname.
	 * 
	 * @param firstname
	 */
	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	/**
	 * Return the user email address.
	 * 
	 * @return
	 */
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * Set the user email address.
	 * 
	 * @param emailAddress
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	/**
	 * Return the username.
	 * 
	 * @return
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the username.
	 * 
	 * @param username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Return the user reference node.
	 * 
	 * @return
	 */
	public NodeReference getNodeReference() {
		return nodeReference;
	}

	/**
	 * Set the user reference node.
	 * 
	 * @param nodeReference
	 */
	public void setNodeReference(NodeReference nodeReference) {
		this.nodeReference = nodeReference;
	}

}
