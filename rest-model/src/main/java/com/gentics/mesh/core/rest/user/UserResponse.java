package com.gentics.mesh.core.rest.user;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.node.NodeResponse;

/**
 * POJO for user response model.
 */
public class UserResponse extends AbstractGenericRestResponse {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Lastname of the user.")
	private String lastname;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Firstname of the user.")
	private String firstname;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Username of the user.")
	private String username;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Email address of the user")
	private String emailAddress;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Optional node reference of the user. Users can directly reference a single node. This can be used to store additional data that is user related.")
	private ExpandableNode nodeReference;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether the user is enabled or disabled. Disabled users can no longer log into Gentics Mesh. Deleting a user user will not remove it. Instead the user will just be disabled.")
	private Boolean enabled;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Hash of roles the user has.")
	private String rolesHash;

	@JsonProperty(required = true)
	@JsonPropertyDescription("List of group references to which the user belongs.")
	private List<GroupReference> groups = new ArrayList<>();

	public UserResponse() {
	}

	/**
	 * Return the lastname of the user.
	 *
	 * @return Lastname of the user
	 */
	public String getLastname() {
		return lastname;
	}

	/**
	 * Set the lastname of the user.
	 *
	 * @param lastname
	 *            Lastname of the user
	 * @return Fluent API
	 */
	public UserResponse setLastname(String lastname) {
		this.lastname = lastname;
		return this;
	}

	/**
	 * Return the firstname.
	 *
	 * @return Firstname of the user
	 */
	public String getFirstname() {
		return firstname;
	}

	/**
	 * Set the firstname.
	 *
	 * @param firstname
	 *            Firstname of the user
	 * @return Fluent API
	 */
	public UserResponse setFirstname(String firstname) {
		this.firstname = firstname;
		return this;
	}

	/**
	 * Returns the email address.
	 *
	 * @return Email address of the user
	 */
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * Set the email address.
	 *
	 * @param emailAddress
	 *            Email address of the user
	 * @return Fluent API
	 */
	public UserResponse setEmailAddress(String emailAddress) {
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
	public UserResponse setUsername(String username) {
		this.username = username;
		return this;

	}
	/**
	 * Returns a hash of the users roles.
	 *
	 * @return A hash of the users roles
	 */
	public String getRolesHash() {
		return rolesHash;
	}

	/**
	 * Set the hash of the users roles.
	 *
	 * @param rolesHash
	 *            Hash of the users roles
	 * @return Fluent API
	 */
	public UserResponse setRolesHash(String rolesHash) {
		this.rolesHash = rolesHash;
		return this;
	}

	/**
	 * Returns the group references of the user.
	 *
	 * @return List of group references of the user.
	 */
	public List<GroupReference> getGroups() {
		return groups;
	}

	/**
	 * Set the groups.
	 *
	 * @param groups
	 *            Groups of the user
	 * @return Fluent API
	 */
	public UserResponse setGroups(List<GroupReference> groups) {
		this.groups = groups;
		return this;
	}

	/**
	 * Return the node reference that was assigned to the user.
	 *
	 * @return Node reference or null if no reference has been set
	 */
	public ExpandableNode getNodeReference() {
		return nodeReference;
	}

	@JsonIgnore
	public NodeResponse getExpandedNodeReference() {
		return (NodeResponse) nodeReference;
	}

	@JsonIgnore
	public NodeReference getReferencedNodeReference() {
		return (NodeReference) nodeReference;
	}

	@JsonIgnore
	public boolean isReference() {
		return (nodeReference instanceof NodeReference);
	}

	@JsonIgnore
	public boolean isExpanded() {
		return (nodeReference instanceof NodeResponse);
	}

	/**
	 * Set the node reference to the user.
	 *
	 * @param nodeReference
	 * @return Fluent API
	 */
	public UserResponse setNodeReference(ExpandableNode nodeReference) {
		this.nodeReference = nodeReference;
		return this;
	}

	/**
	 * Set the expanded node response.
	 *
	 * @param nodeResponse
	 * @return Fluent API
	 */
	public UserResponse setNodeResponse(NodeResponse nodeResponse) {
		this.nodeReference = nodeResponse;
		return this;
	}

	/**
	 * Return the enabled flag for the user.
	 *
	 * @return Enabled flag
	 */
	public Boolean getEnabled() {
		return enabled;
	}

	/**
	 * Set the enabled flag for the user.
	 *
	 * @param enabled
	 *            Enabled flag
	 * @return Fluent API
	 */
	public UserResponse setEnabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

}
