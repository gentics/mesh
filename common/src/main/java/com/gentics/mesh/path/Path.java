package com.gentics.mesh.path;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * A webroot path consists of multiple segments. This class provides a useful container which can be used when resolving webroot paths.
 */
public class Path {

	private List<PathSegment> segments = new ArrayList<>();

	private String targetPath;

	private Stack<String> initialStack;

	private boolean prefixMismatch = false;

	/**
	 * Return the list of segments which describe the path.
	 * 
	 * @return
	 */
	public List<PathSegment> getSegments() {
		return segments;
	}

	/**
	 * Add the path segment to the path.
	 * 
	 * @param segment
	 * @return Fluent API
	 */
	public Path addSegment(PathSegment segment) {
		segments.add(segment);
		return this;
	}

	/**
	 * Returns the last segment of the path.
	 * 
	 * @return Found segment or null when the path has no segments
	 */
	public PathSegment getLast() {
		if (segments.size() == 0) {
			return null;
		}
		return segments.get(segments.size() - 1);
	}

	/**
	 * Returns first segment of the path.
	 * 
	 * @return Found segment or null when the path has no segments
	 */
	public PathSegment getFirst() {
		if (segments.size() == 0) {
			return null;
		}
		return segments.get(0);
	}

	/**
	 * Return the target path.
	 * 
	 * @return
	 */
	public String getTargetPath() {
		return targetPath;
	}

	/**
	 * Set the target path.
	 * 
	 * @param targetPath
	 * @return Fluent API
	 */
	public Path setTargetPath(String targetPath) {
		this.targetPath = targetPath;
		return this;
	}

	public boolean isFullyResolved() {
		// We assume the path is fully resolved if no initial stack was set.
		// In some places a path is created without the need of a resolve stack (e.g. url field handling)
		return initialStack == null || initialStack.size() == segments.size();
	}

	/**
	 * Return the resolved path.
	 * 
	 * @return
	 */
	public String getResolvedPath() {
		return "/" + String.join("/", getSegments().stream().map(PathSegment::getSegment).collect(Collectors.toList()));
	}

	public void setInitialStack(Stack<String> stack) {
		this.initialStack = stack;
	}

	public Stack<String> getInitialStack() {
		return initialStack;
	}

	/**
	 * Set the prefix mismatch flag in the path to indicate that the prefix was wrong.
	 * 
	 * @param flag
	 */
	public void setPrefixMismatch(boolean flag) {
		this.prefixMismatch = flag;
	}

	public boolean isPrefixMismatch() {
		return prefixMismatch;
	}

}
