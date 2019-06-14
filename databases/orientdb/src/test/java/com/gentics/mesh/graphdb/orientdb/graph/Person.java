package com.gentics.mesh.graphdb.orientdb.graph;

import java.util.List;

import com.gentics.madl.annotations.GraphElement;

@GraphElement
public class Person extends AbstractInterceptingVertexFrame {

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
