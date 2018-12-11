package com.gentics.mesh.error;

public abstract class AbstractElementNotFoundException extends RuntimeException {

	private static final long serialVersionUID = 7023422305585846080L;
	private Object id;
	private Class<?> clazz;

	public AbstractElementNotFoundException(Object id, Class<?> clazz) {
		this.id = id;
		this.clazz = clazz;
	}

	@Override
	public String getMessage() {
		return "No element for Id {" + id + "} of type {" + clazz.getName() + "} could be found within the graph";
	}

	/**
	 * Return the id of the element that was expected.
	 * 
	 * @return
	 */
	public Object getId() {
		return id;
	}

	/**
	 * Return the element class of the element which was expected.
	 * 
	 * @return
	 */
	public Class<?> getElementClass() {
		return clazz;
	}

}
