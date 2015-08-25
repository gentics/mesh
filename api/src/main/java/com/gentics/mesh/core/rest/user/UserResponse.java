package com.gentics.mesh.core.rest.user;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;

/**
 * User response model.
 */
public class UserResponse extends AbstractGenericNodeRestModel {

	private String lastname;

	private String firstname;

	private String username;

	private String emailAddress;

	private NodeReference nodeReference;

	//TODO we should use a reference here to include name and uuid
	private List<String> groups = new ArrayList<>();

	public UserResponse() {
	}

	/**
	 * Return the lastname of the user.
	 * 
	 * @return
	 */
	public String getLastname() {
		return lastname;
	}

	/**
	 * Set the lastname of the user.
	 * 
	 * @param lastname
	 */
	public void setLastname(String lastname) {
		this.lastname = lastname;
	}

	/**
	 * Return the firstname.
	 * 
	 * @return
	 */
	public String getFirstname() {
		return firstname;
	}

	/**
	 * Set the firstname.
	 * 
	 * @param firstname
	 */
	public void setFirstname(String firstname) {
		this.firstname = firstname;
	}

	/**
	 * Returns the email address.
	 * 
	 * @return
	 */
	public String getEmailAddress() {
		return emailAddress;
	}

	/**
	 * Set the email address.
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
	 * Returns the groups of the user.
	 * 
	 * @return
	 */
	public List<String> getGroups() {
		return groups;
	}

	/**
	 * Add the given group name to the list of groups.
	 * 
	 * @param name
	 */
	public void addGroup(String name) {
		this.groups.add(name);
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
}
