package com.gentics.mesh.core.data.model.tinkerpop;

import java.util.List;

import com.gentics.mesh.core.data.model.auth.AuthRelationships;
import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;

public class User extends GenericNode {

	public static String FIRSTNAME_KEY = "firstname";

	public static String LASTNAME_KEY = "lastname";

	public static String USERNAME_KEY = "username";

	public static String EMAIL_KEY = "emailAddress";

	public static String PASSWORD_HASH_KEY = "passwordHash";

	public String getFirstname() {
		return getProperty(FIRSTNAME_KEY);
	}

	public void setFirstname(String name) {
		setProperty(FIRSTNAME_KEY, name);
	}

	public String getLastname() {
		return getProperty(LASTNAME_KEY);
	}

	public void setLastname(String name) {
		setProperty(LASTNAME_KEY, name);
	}

	// TODO add unique index
	public String getUsername() {
		return getProperty(USERNAME_KEY);
	}

	public void setUsername(String name) {
		setProperty(USERNAME_KEY, name);
	}

	public String getEmailAddress() {
		return getProperty(EMAIL_KEY);
	}

	public void setEmailAddress(String emailAddress) {
		setProperty(EMAIL_KEY, emailAddress);
	}

	public List<Group> getGroups() {
		return out(AuthRelationships.HAS_USER).toList(Group.class);
	}

	public void addGroup(Group group) {
		linkOut(group, AuthRelationships.HAS_USER);
	}

	public Long getGroupCount() {
		return out(BasicRelationships.HAS_USER).count();
	}

	public String getPasswordHash() {
		return getProperty(PASSWORD_HASH_KEY);
	}

	public void setPasswordHash(String hash) {
		setProperty(PASSWORD_HASH_KEY, hash);
	}

	public String getPrincipalId() {
		return getUsername() + "%" + getEmailAddress() + "%" + getPasswordHash() + "#" + getId();
	}

}
