package com.gentics.mesh.graphdb.orientdb.graph;

import com.gentics.madl.annotations.GraphElement;
import com.gentics.madl.frame.AbstractVertexFrame;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.core.result.TraversalResult;

/**
 * Test entity
 */
@GraphElement
public class Person extends AbstractVertexFrame {

	public TraversalResult<? extends Person> getFriends() {
		return new TraversalResult<>(out("HAS_FRIEND", Person.class));
	}

	/**
	 * Add the friend to the person.
	 * 
	 * @param person
	 */
	public void addFriend(Person person) {
		linkOut(person, "HAS_FRIEND");
	}

	@Setter
	public void setName(String name) {
		setProperty("name", name);
	}

	public String getName() {
		return getProperty("name");
	}
}
