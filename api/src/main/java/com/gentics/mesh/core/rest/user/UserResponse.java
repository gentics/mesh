package com.gentics.mesh.core.rest.user;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.group.GroupReference;

/**
 * User response model.
 */
public class UserResponse extends AbstractGenericRestResponse {

	private String lastname;

	private String firstname;

	private String username;

	private String emailAddress;

	private NodeReference nodeReference;

	private boolean enabled;

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
	 */
	public void setLastname(String lastname) {
		this.lastname = lastname;
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
	 */
	public void setFirstname(String firstname) {
		this.firstname = firstname;
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
	 */
	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
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
	 */
	public void setUsername(String username) {
		this.username = username;
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
	 * @return
	 */
	public NodeReference getNodeReference() {
		return nodeReference;
	}

	/**
	 * Set the node reference to the user.
	 * 
	 * @param nodeReference
	 */
	public void setNodeReference(NodeReference nodeReference) {
		this.nodeReference = nodeReference;
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
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
}
