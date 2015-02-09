package com.gentics.cailun.core.verticle;

public class ContentNotFoundException extends Exception {

	public ContentNotFoundException() {
		super("Content could not be found");
	}

	public ContentNotFoundException(String uuid) {
		super("Content with uuid {" + uuid + "} could not be found.");
	}
}
