package com.gentics.mesh.core.rest.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a user update request.
 */
public class UserUpdateRequest implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("New password of the user")
	private String password;

	private String oldPassword;

	@JsonProperty(required = false)
	@JsonPropertyDescription("New lastname of the user")
	private String lastname;

	@JsonProperty(required = false)
	@JsonPropertyDescription("New firstname of the user")
	private String firstname;

	@JsonProperty(required = false)
	@JsonPropertyDescription("New username of the user")
	private String username;

	@JsonProperty(required = false)
	@JsonPropertyDescription("New email address of the user")
	private String emailAddress;

	@JsonProperty(required = false)
	@JsonPropertyDescription("New node reference of the user. This can also explicitly set to null in order to remove the assigned node from the user")
	private ExpandableNode nodeReference;

	@JsonProperty(required = false)
	@JsonPropertyDescription("When true, the user needs to change their password on the next login.")
	private Boolean forcedPasswordChange;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether the user is an admin.")
	private Boolean admin;

	public UserUpdateRequest() {
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
	 * Return the old password which is needed to confirm permission to update the currently set password.
	 * 
	 * @return
	 */
	public String getOldPassword() {
		return oldPassword;
	}

	/**
	 * Set the old password which is needed to confirm the permission to update the currently set password.
	 * 
	 * @param oldPassword
	 */
	public void setOldPassword(String oldPassword) {
		this.oldPassword = oldPassword;
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
	public ExpandableNode getNodeReference() {
		return nodeReference;
	}

	/**
	 * Set the user reference node which can be used to store additional user specific data.
	 * 
	 * @param nodeReference
	 *            Node reference of the user
	 * @return Fluent API
	 */
	public UserUpdateRequest setNodeReference(ExpandableNode nodeReference) {
		this.nodeReference = nodeReference;
		return this;
	}

	/**
	 * Return true if the user needs to change their password on next login.
	 *
	 * @return
	 */
	public Boolean getForcedPasswordChange() {
		return forcedPasswordChange;
	}

	/**
	 * Set whether the user needs to change their password on next login.
	 *
	 * @param forcedPasswordChange
	 * @return Fluent API
	 */
	public UserUpdateRequest setForcedPasswordChange(Boolean forcedPasswordChange) {
		this.forcedPasswordChange = forcedPasswordChange;
		return this;
	}

	/**
	 * Return the admin flag.
	 * 
	 * @return
	 */
	public Boolean getAdmin() {
		return admin;
	}

	/**
	 * Set the user update flag. Note that only admins can alter the flag.
	 * 
	 * @param flag
	 */
	public void setAdmin(Boolean flag) {
		this.admin = flag;
	}

}
