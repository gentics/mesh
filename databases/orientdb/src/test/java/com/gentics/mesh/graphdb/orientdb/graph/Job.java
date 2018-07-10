package com.gentics.mesh.graphdb.orientdb.graph;

import java.util.stream.Stream;

import com.syncleus.ferma.annotations.GraphElement;
import com.syncleus.ferma.ext.AbstractInterceptingVertexFrame;

@GraphElement
public class Job extends AbstractInterceptingVertexFrame {

	public Stream<? extends Person> getEmployee() {
		return out("HAS_EMPLOYEE").stream(Person.class);
	}

	public void addEmployee(Person person) {
		addFramedEdge("HAS_EMPLOYEE", person);
	}
}
