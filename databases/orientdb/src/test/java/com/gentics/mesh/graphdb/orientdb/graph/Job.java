package com.gentics.mesh.graphdb.orientdb.graph;

import java.util.List;

import com.gentics.madl.annotation.GraphElement;
import com.gentics.madl.wrapper.element.AbstractWrappedVertex;

@GraphElement
public class Job extends AbstractWrappedVertex {

	public List<? extends Person> getEmployee() {
		return out("HAS_EMPLOYEE").frameExplicit(Person.class).list();
	}

	public void addEmployee(Person person) {
		addEdgeOut(person, "HAS_EMPLOYEE");
	}
}
