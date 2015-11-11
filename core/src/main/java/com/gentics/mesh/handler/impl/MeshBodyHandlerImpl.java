package com.gentics.mesh.handler.impl;

import io.vertx.ext.web.handler.impl.BodyHandlerImpl;

/**
 * Wrapper class to avoid stupid default constructor behavior. 
 */
public class MeshBodyHandlerImpl extends BodyHandlerImpl {

	public MeshBodyHandlerImpl(String uploadsDirectory) {
		setUploadsDirectory(uploadsDirectory);
	}
}
