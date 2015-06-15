package com.gentics.mesh.core.verticle;

//TODO do we really need this class?
public class NodeNotFoundException extends Exception {

	private static final long serialVersionUID = 8181246038039956866L;

	public NodeNotFoundException() {
		super("Node could not be found");
	}

	public NodeNotFoundException(String uuid) {
		super("Node with uuid {" + uuid + "} could not be found.");
	}
}
