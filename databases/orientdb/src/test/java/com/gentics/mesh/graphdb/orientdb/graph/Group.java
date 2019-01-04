package com.gentics.mesh.graphdb.orientdb.graph;

import java.util.List;

import com.gentics.madl.annotation.GraphElement;
import com.gentics.madl.wrapper.element.AbstractWrappedVertex;

@GraphElement
public class Group extends AbstractWrappedVertex {

	public List<? extends Person> getMembers() {
		return out("HAS_MEMBER").frameExplicit(Person.class).list();
	}

	public void addMember(Person person) {
		addEdgeOut(person, "HAS_MEMBER");
	}

	public void setName(String name) {
		property("name", name);
	}

	public String getName() {
		return value("name");
	}

}
