package com.gentics.cailun.core.rest.model.auth;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.rest.model.AbstractPersistable;

@NoArgsConstructor
@Data
@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
@NodeEntity
public class User extends AbstractPersistable {

	private static final long serialVersionUID = -8707906688270506022L;

	private String lastname;

	private String firstname;

	private String username;

	private String emailAddress;

	private String passwordHash;

	// @Fetch
	// @RelatedTo(type = "MEMBER_OF", direction = Direction.OUTGOING, elementClass = Group.class)
	// private Set<Group> groups = new HashSet<>();

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
