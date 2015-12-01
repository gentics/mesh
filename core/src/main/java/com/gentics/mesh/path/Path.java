package com.gentics.mesh.path;

import java.util.ArrayList;
import java.util.List;

public class Path {

	private List<PathSegment> segments = new ArrayList<>();

	private String targetPath;

	public List<PathSegment> getSegments() {
		return segments;
	}

	public void addSegment(PathSegment segment) {
		segments.add(segment);
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

	public String getTargetPath() {
		return targetPath;
	}

	public void setTargetPath(String targetPath) {
		this.targetPath = targetPath;
	}
}
