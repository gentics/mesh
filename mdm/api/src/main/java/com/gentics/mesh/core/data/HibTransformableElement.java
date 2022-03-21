package com.gentics.mesh.core.data;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * An interface for elements that can be transformed to a rest model.
 */
public interface HibTransformableElement<T extends RestModel> extends HibBaseElement {

	/**
	 * Return the API path to the element.
	 * 
	 * @param ac
	 * 
	 * @return API path or null if the element has no public path
	 */
	String getAPIPath(InternalActionContext ac);

	/**
	 * Return the etag for the element.
	 * 
	 * @param ac
	 * @return Generated etag
	 */
	String getETag(InternalActionContext ac);

}
