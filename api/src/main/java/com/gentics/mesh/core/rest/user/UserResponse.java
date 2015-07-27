package com.gentics.mesh.core.rest.user;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;

public class UserResponse extends AbstractGenericNodeRestModel {

	private String lastname;

	private String firstname;

	private String username;

	private String emailAddress;

	private NodeReference nodeReference;

	private List<String> groups = new ArrayList<>();

	public UserResponse() {
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

	public List<String> getGroups() {
		return groups;
	}

	public void setGroups(List<String> groups) {
		this.groups = groups;
	}

	public void addGroup(String name) {
		this.groups.add(name);
	}

	public NodeReference getNodeReference() {
		return nodeReference;
	}

	public void setNodeReference(NodeReference nodeReference) {
		this.nodeReference = nodeReference;
	}
}
