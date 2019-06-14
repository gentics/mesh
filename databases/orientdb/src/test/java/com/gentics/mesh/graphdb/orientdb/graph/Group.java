package com.gentics.mesh.graphdb.orientdb.graph;

import java.util.List;

import com.gentics.madl.annotations.GraphElement;

@GraphElement
public class Group extends AbstractInterceptingVertexFrame {

	public List<? extends Person> getMembers() {
		return out("HAS_MEMBER").has(Person.class).toListExplicit(Person.class);
	}

	public void addMember(Person person) {
		linkOut(person, "HAS_MEMBER");
	}

	public void setName(String name) {
		setProperty("name", name);
	}

	public String getName() {
		return getProperty("name");
	}

}
