package com.gentics.mesh.util;

import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.util.wrappers.wrapped.WrappedElement;

public final class GraphUtil {
	private GraphUtil() {}

	/**
	 * Checks if the graph element is a faulty wrapped element.
	 *
	 * A wrapped element is faulty if the wrapped object is null. This usually should not happen,
	 * but OrientDB sometimes still returns these elements. This function provides a workaround
	 * for these cases.
	 *
	 * @param element The element to be checked
	 * @return false if the element is a wrapped element and faulty. True in any other case.
	 */
	public static boolean filterFaultyElement(Element element) {
		if (element instanceof WrappedElement) {
			return ((WrappedElement)element).getBaseElement() != null;
		} else {
			return true;
		}
	}
}
