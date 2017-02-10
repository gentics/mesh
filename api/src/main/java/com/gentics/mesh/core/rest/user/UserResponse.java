package com.gentics.mesh.core.rest.user;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.node.NodeResponse;

/**
 * User response model.
 */
public class UserResponse extends AbstractGenericRestResponse {

	@JsonPropertyDescription("Lastname of the user.")
	private String lastname;

	@JsonPropertyDescription("Firstname of the user.")
	private String firstname;

	@JsonPropertyDescription("Username of the user.")
	private String username;

	@JsonPropertyDescription("Email address of the user")
	private String emailAddress;

	@JsonPropertyDescription("Optional node reference of the user. Users can directly reference a single node. This can be used to store additional data that is user related.")
	private ExpandableNode nodeReference;

	@JsonPropertyDescription("Flag which indicates whether the user is enabled or disabled. Deleting a user will disable it.")
	private boolean enabled;

	@JsonPropertyDescription("List of groups to which the user belongs")
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
	 * Returns the group references of the user.
	 * 
	 * @return List of group references of the user.
	 */
	public List<GroupReference> getGroups() {
		return groups;
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
	public boolean getEnabled() {
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
