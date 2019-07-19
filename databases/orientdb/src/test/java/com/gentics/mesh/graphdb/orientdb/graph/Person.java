package com.gentics.mesh.graphdb.orientdb.graph;

import com.gentics.madl.annotations.GraphElement;
import com.gentics.madl.frame.AbstractVertexFrame;
import com.gentics.mesh.madl.traversal.TraversalResult;

@GraphElement
public class Person extends AbstractVertexFrame {

	public TraversalResult<? extends Person> getFriends() {
		return new TraversalResult<>(out("HAS_FRIEND").has(Person.class).frameExplicit(Person.class));
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
