package com.gentics.mesh.path;

import java.util.List;
import java.util.Stack;

public interface Path {

	/**
	 * Set the prefix mismatch flag in the path to indicate that the prefix was wrong.
	 * 
	 * @param flag
	 */
	void setPrefixMismatch(boolean flag);

	Stack<String> getInitialStack();

	void setInitialStack(Stack<String> stack);

	/**
	 * Return the resolved path.
	 * 
	 * @return
	 */
	String getResolvedPath();

	boolean isFullyResolved();

	/**
	 * Set the target path.
	 * 
	 * @param targetPath
	 * @return Fluent API
	 */
	Path setTargetPath(String targetPath);

	/**
	 * Return the target path.
	 * 
	 * @return
	 */
	String getTargetPath();

	/**
	 * Return the list of segments which describe the path.
	 * 
	 * @return
	 */
	List<PathSegment> getSegments();

	/**
	 * Add the path segment to the path.
	 * 
	 * @param segment
	 * @return Fluent API
	 */
	Path addSegment(PathSegment segment);

	/**
	 * Check whether all segments in the path are valid.
	 * 
	 * @return
	 */
	boolean isValid();

	/**
	 * Returns first segment of the path.
	 * 
	 * @return Found segment or null when the path has no segments
	 */
	PathSegment getFirst();

	/**
	 * Returns the last segment of the path.
	 * 
	 * @return Found segment or null when the path has no segments
	 */
	PathSegment getLast();

	boolean isPrefixMismatch();

}
