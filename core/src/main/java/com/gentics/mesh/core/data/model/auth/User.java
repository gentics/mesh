package com.gentics.mesh.core.data.model.auth;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.mesh.core.data.model.generic.GenericNode;

@NodeEntity
public class User extends GenericNode {

	private static final long serialVersionUID = -8707906688270506022L;

	private String lastname;

	private String firstname;

	@Indexed(unique = true)
	private String username;

	private String emailAddress;

	private String passwordHash;

	@RelatedTo(type = AuthRelationships.MEMBER_OF, direction = Direction.OUTGOING, elementClass = Group.class)
	private Set<Group> groups = new HashSet<>();

	@SuppressWarnings("unused")
	private User() {

	}

	/**
	 * Create a new user with the given username.
	 * 
	 * @param username
	 */
	public User(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
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

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getPrincipalId() {
		return username + "%" + emailAddress + "%" + passwordHash + "#" + getId();
	}

	/**
	 * Please note that the {@link User#toString()} method is currently used to identify the principal for authorization.
	 * 
	 * @return
	 */
	public String toString() {
		return getPrincipalId();
	}

	public Set<Group> getGroups() {
		return groups;
	}

}
