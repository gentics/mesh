package com.gentics.mesh.graphdb.orientdb;

import java.util.List;

import com.syncleus.ferma.AbstractVertexFrame;

public class Job extends AbstractVertexFrame {

	public List<? extends Person> getEmployee() {
		return out("HAS_EMPLOYEE").toListExplicit(Person.class);
	}

	public void addEmployee(Person person) {
		addFramedEdge("HAS_EMPLOYEE", person);
	}
}
