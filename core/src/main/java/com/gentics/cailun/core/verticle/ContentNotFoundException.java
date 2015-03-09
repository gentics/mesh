package com.gentics.cailun.core.verticle;

//TODO do we really need this class?
public class ContentNotFoundException extends Exception {

	private static final long serialVersionUID = 8181246038039956866L;

	public ContentNotFoundException() {
		super("Content could not be found");
	}

	public ContentNotFoundException(String uuid) {
		super("Content with uuid {" + uuid + "} could not be found.");
	}
}
