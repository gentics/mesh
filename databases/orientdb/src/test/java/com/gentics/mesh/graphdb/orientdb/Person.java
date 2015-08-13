package com.gentics.mesh.graphdb.orientdb;

import java.util.List;

import com.syncleus.ferma.AbstractVertexFrame;

public class Person extends AbstractVertexFrame {

	public List<? extends Person> getFriends() {
		return out("HAS_FRIEND").has(Person.class).toListExplicit(Person.class);
	}

	public void addFriend(Person person) {
		linkOut(person, "HAS_FRIEND");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	public String getName() {
		return getProperty("name");
	}

}
