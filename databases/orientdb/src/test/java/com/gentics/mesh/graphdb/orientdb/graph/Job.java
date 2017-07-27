package com.gentics.mesh.graphdb.orientdb.graph;

import java.util.List;

import com.syncleus.ferma.annotations.GraphElement;

@GraphElement
public class Job extends AbstractInterceptingVertexFrame {

	public List<? extends Person> getEmployee() {
		return out("HAS_EMPLOYEE").toListExplicit(Person.class);
	}

	public void addEmployee(Person person) {
		addFramedEdge("HAS_EMPLOYEE", person);
	}
}
