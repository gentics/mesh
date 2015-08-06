package com.gentics.mesh.json;

import com.fasterxml.jackson.core.JsonProcessingException;

public class MeshJsonException extends JsonProcessingException {

	private static final long serialVersionUID = 9006740094619181296L;

	public MeshJsonException(String msg) {
		super(msg);
	}

	public MeshJsonException(String msg, Exception e) {
		super(msg, e);
	}

}
