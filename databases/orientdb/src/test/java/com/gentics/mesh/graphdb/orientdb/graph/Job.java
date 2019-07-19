package com.gentics.mesh.graphdb.orientdb.graph;

import com.gentics.madl.annotations.GraphElement;
import com.gentics.madl.frame.AbstractVertexFrame;

@GraphElement
public class Job extends AbstractVertexFrame {

	public void addEmployee(Person person) {
		addFramedEdge("HAS_EMPLOYEE", person);
	}
}
