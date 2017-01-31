package com.gentics.mesh.error;

/**
 * Exception which is raised if configuration errors have been detected.
 */
public class MeshConfigurationException extends Exception {

	private static final long serialVersionUID = -5888052417243276295L;

	public MeshConfigurationException(String msg) {
		super(msg);
	}

	public MeshConfigurationException(String msg, Throwable t) {
		super(msg, t);
	}
}
