package com.gentics.mesh.error;

public class VertexNotFoundException extends AbstractElementNotFoundException {

	private static final long serialVersionUID = 2036158307789877660L;

	public VertexNotFoundException(Object id, Class<?> clazz) {
		super(id, clazz);
	}

}
