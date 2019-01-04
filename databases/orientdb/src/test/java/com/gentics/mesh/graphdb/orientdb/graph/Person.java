package com.gentics.mesh.graphdb.orientdb.graph;

import java.util.List;

import com.gentics.madl.annotation.GraphElement;
import com.gentics.madl.wrapper.element.AbstractWrappedVertex;

@GraphElement
public class Person extends AbstractWrappedVertex {

	public List<? extends Person> getFriends() {
		return out("HAS_FRIEND").frameExplicit(Person.class).list();
	}

	public void addFriend(Person person) {
		addEdgeOut(person, "HAS_FRIEND");
	}

	public void setName(String name) {
		property("name", name);
	}

	public String getName() {
		return value("name");
	}

}
