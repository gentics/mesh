package com.gentics.mesh.core.data;

public interface HandleElementAction {

	/**
	 * Invoke the handler action.
	 * 
	 * @param element
	 *            Element to be handled
	 * @param context
	 *            Context to be used when handling the element
	 */
	void call(IndexableElement element, HandleContext context);

}
