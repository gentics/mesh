package com.gentics.mesh.path;

import java.util.List;
import java.util.Stack;

/**
 * A path is the result object of a path resolve operation. It lists the resolved segments and has information of the stack that was used during the resolve
 * operation.
 */
public interface Path {

	/**
	 * Set the prefix mismatch flag in the path to indicate that the prefix was wrong.
	 * 
	 * @param flag
	 */
	void setPrefixMismatch(boolean flag);

	/**
	 * Return the resolve stack which contains the segmented path strings
	 * 
	 * @return
	 */
	Stack<String> getInitialStack();

	/**
	 * Set the resolve stack which contains path segment strings.
	 * 
	 * @param stack
	 */
	void setInitialStack(Stack<String> stack);

	/**
	 * Return the resolved path.
	 * 
	 * @return
	 */
	String getResolvedPath();

	/**
	 * Flag which indicates whether the path resolve operation was able to fully resolve the path. When segments can't be found or resolved this will return
	 * false.
	 * 
	 * @return
	 */
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

	/**
	 * Check whether the branch/project prefix was matched during the resolve operation.
	 * 
	 * @return
	 */
	boolean isPrefixMismatch();

}
