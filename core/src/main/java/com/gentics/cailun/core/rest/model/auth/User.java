package com.gentics.cailun.core.rest.model.auth;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.cailun.core.rest.model.generic.GenericNode;

@NoArgsConstructor
@Data
@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
@NodeEntity
public class User extends GenericNode {

	private static final long serialVersionUID = -8707906688270506022L;

	private String lastname;

	private String firstname;

	private String username;

	private String emailAddress;

	private String passwordHash;

	/**
	 * Create a new user with the given username.
	 * 
	 * @param username
	 */
	public User(String username) {
		this.username = username;
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

}
