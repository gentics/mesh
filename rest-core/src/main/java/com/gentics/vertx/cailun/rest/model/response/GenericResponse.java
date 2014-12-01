package com.gentics.vertx.cailun.rest.model.response;

public class GenericResponse<T> {

	T object;

	public GenericResponse(T object) {
		this.object = object;
	}

	public GenericResponse() {
	}

	public T getObject() {
		return object;
	}

	public void setObject(T object) {
		this.object = object;
	}

}
