package com.gentics.mesh.core.rest.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a user create request model.
 */
public class UserCreateRequest implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Username of the user.")
	private String username;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Email address of the user.")
	private String emailAddress;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Lastname of the user.")
	private String lastname;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Firstname of the user.")
	private String firstname;

	@JsonPropertyDescription("Optional group id for the user. If provided the user will automatically be assigned to the identified group.")
	@JsonProperty(required = false)
	private String groupUuid;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Password of the new user.")
	private String password;

	@JsonProperty(required = false)
	@JsonPropertyDescription("New node reference of the user. This can also explicitly set to null in order to remove the assigned node from the user")
	private ExpandableNode nodeReference;

	@JsonProperty(required = false)
	@JsonPropertyDescription("When true, the user needs to change their password on the next login.")
	private Boolean forcedPasswordChange;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether the user is an admin.")
	private Boolean admin;

	public UserCreateRequest() {
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
	public UserCreateRequest setUsername(String username) {
		this.username = username;
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
	public UserCreateRequest setFirstname(String firstname) {
		this.firstname = firstname;
		return this;
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
	public UserCreateRequest setLastname(String lastname) {
		this.lastname = lastname;
		return this;
	}

	/**
	 * Set the user email address.
	 * 
	 * @param emailAddress
	 * @return Fluent API
	 */
	public UserCreateRequest setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
		return this;
	}

	/**
	 * Return the group uuid for the group to which the user should be assigned.
	 * 
	 * @return
	 */
	public String getGroupUuid() {
		return groupUuid;
	}

	/**
	 * Set the group uuid for the group to which the user should be assigned.
	 * 
	 * @param groupUuid
	 */
	public void setGroupUuid(String groupUuid) {
		this.groupUuid = groupUuid;
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
	public UserCreateRequest setPassword(String password) {
		this.password = password;
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
	public UserCreateRequest setNodeReference(ExpandableNode nodeReference) {
		this.nodeReference = nodeReference;
		return this;
	}

	/**
	 * Set whether the user needs to change their password on next login.
	 *
	 * @param forcedPasswordChange
	 * @return Fluent API
	 */
	public UserCreateRequest setForcedPasswordChange(Boolean forcedPasswordChange) {
		this.forcedPasswordChange = forcedPasswordChange;
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
	 * Return the admin flag for the user.
	 * 
	 * @return
	 */
	public Boolean getAdmin() {
		return admin;
	}

	/**
	 * Set the admin flag on the user. Note that only admins can create admin users.
	 * 
	 * @param flag
	 */
	public void setAdmin(Boolean flag) {
		this.admin = flag;
	}
}
