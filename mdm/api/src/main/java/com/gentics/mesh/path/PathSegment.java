package com.gentics.mesh.path;

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

}
