package com.gentics.mesh.core.data;

import com.gentics.mesh.core.data.search.context.GenericEntryContext;

public interface HandleElementAction {

	/**
	 * Invoke the handler action.
	 * 
	 * @param element
	 *            Element to be handled
	 * @param context
	 *            Context to be used when handling the element
	 */
	void call(IndexableElement element, GenericEntryContext context);

}
