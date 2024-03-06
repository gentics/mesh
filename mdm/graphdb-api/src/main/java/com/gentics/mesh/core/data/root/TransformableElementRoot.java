package com.gentics.mesh.core.data.root;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * A {@link TransformableElementRoot} is a node that can be transformed into a rest model response.
 *
 * @param <T>
 *            RestModel response class
 */
public interface TransformableElementRoot<T, R extends RestModel> {

	/**
	 * Return the API path to the element.
	 *
	 * @param element
	 * @param ac
	 * 
	 * @return API path or null if the element has no public path
	 */
	String getAPIPath(T element, InternalActionContext ac);

	/**
	 * Return the etag for the element.
	 *
	 * @param element
	 * @param ac
	 * @return Generated etag
	 */
	String getETag(T element, InternalActionContext ac);

}
