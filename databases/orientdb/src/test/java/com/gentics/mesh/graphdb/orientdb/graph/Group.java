package com.gentics.mesh.graphdb.orientdb.graph;

import java.util.Iterator;

import org.apache.tinkerpop.gremlin.structure.Direction;

import com.gentics.madl.annotations.GraphElement;
import com.gentics.madl.frame.AbstractVertexFrame;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.core.result.TraversalResult;

/**
 * Test entity
 */
@GraphElement
public class Group extends AbstractVertexFrame {

	public TraversalResult<? extends Person> getMembers() {
		return new TraversalResult<>((Iterator) getElement().vertices(Direction.OUT, "HAS_MEMBER"));
	}

	/**
	 * Add the person to the group.
	 * 
	 * @param person
	 */
	public void addMember(Person person) {
		linkOut(person, "HAS_MEMBER");
	}

	@Setter
	public void setName(String name) {
		setProperty("name", name);
	}

	public String getName() {
		return getProperty("name");
	}
}
