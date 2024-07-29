package com.gentics.mesh.path;

import com.gentics.mesh.core.data.HibNodeFieldContainer;

/**
 * Resolved path segments for a {@link Path} object.
 */
public interface PathSegment {

	/**
	 * Return the segment which was used to resolve the path segment.
	 * 
	 * @return
	 */
	String getSegment();

	/**
	 * Return the language tag of the node which provides this segment.
	 * 
	 * @return
	 */
	String getLanguageTag();

	/**
	 * Return the container for the segment.
	 * 
	 * @return
	 */
	HibNodeFieldContainer getContainer();
}
