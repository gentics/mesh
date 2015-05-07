package com.gentics.mesh.error;

public class ServerErrorException extends RuntimeException {

	private static final long serialVersionUID = 3256173873587455721L;

	public ServerErrorException(String message) {
		super(message);
	}

}
