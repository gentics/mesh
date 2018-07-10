package com.gentics.mesh.graphdb.orientdb.graph;

import java.util.stream.Stream;

import com.syncleus.ferma.annotations.GraphElement;
import com.syncleus.ferma.ext.AbstractInterceptingVertexFrame;

@GraphElement
public class Person extends AbstractInterceptingVertexFrame {

	public Stream<? extends Person> getFriends() {
		return out("HAS_FRIEND").stream(Person.class);
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
