package com.gentics.mesh.graphdb.orientdb.graph;

import java.util.stream.Stream;

import com.syncleus.ferma.annotations.GraphElement;
import com.syncleus.ferma.ext.AbstractInterceptingVertexFrame;

@GraphElement
public class Group extends AbstractInterceptingVertexFrame {

	public Stream<? extends Person> getMembers() {
		return out("HAS_MEMBER").stream(Person.class);
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
