package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.HibBaseElement;

/**
 * Contains data structure dependent mechanisms.
 * 
 * @author plyhun
 *
 */
public interface RootResolver {

	/**
	 * This method will try to resolve the given path and return the element that is matching the path.
	 * 
	 * @param pathToElement
	 * @return Resolved element or null if no element could be found
	 */
	HibBaseElement resolvePathToElement(String pathToElement);
}
