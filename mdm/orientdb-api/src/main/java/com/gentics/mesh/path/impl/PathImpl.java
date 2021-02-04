package com.gentics.mesh.path.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.stream.Collectors;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * A webroot path consists of multiple segments. This class provides a useful container which can be used when resolving webroot paths.
 */
public class PathImpl implements Path {

	private static final Logger log = LoggerFactory.getLogger(Path.class);

	private List<PathSegment> segments = new ArrayList<>();

	private String targetPath;

	private Stack<String> initialStack;

	private boolean prefixMismatch = false;

	@Override
	public List<PathSegment> getSegments() {
		return segments;
	}

	@Override
	public Path addSegment(PathSegment segment) {
		segments.add(segment);
		return this;
	}

	@Override
	public PathSegment getLast() {
		if (segments.size() == 0) {
			return null;
		}
		return segments.get(segments.size() - 1);
	}

	@Override
	public PathSegment getFirst() {
		if (segments.size() == 0) {
			return null;
		}
		return segments.get(0);
	}

	@Override
	public String getTargetPath() {
		return targetPath;
	}

	@Override
	public Path setTargetPath(String targetPath) {
		this.targetPath = targetPath;
		return this;
	}

	@Override
	public boolean isFullyResolved() {
		// We assume the path is fully resolved if no initial stack was set.
		// In some places a path is created without the need of a resolve stack (e.g. url field handling)
		return initialStack == null || initialStack.size() == segments.size();
	}

	@Override
	public String getResolvedPath() {
		return "/" + String.join("/", getSegments().stream().map(PathSegment::getSegment).collect(Collectors.toList()));
	}

	@Override
	public void setInitialStack(Stack<String> stack) {
		this.initialStack = stack;
	}

	@Override
	public Stack<String> getInitialStack() {
		return initialStack;
	}

	@Override
	public void setPrefixMismatch(boolean flag) {
		this.prefixMismatch = flag;
	}

	@Override
	public boolean isPrefixMismatch() {
		return prefixMismatch;
	}

	@Override
	public boolean isValid() {
		for (PathSegment segment : segments) {
			try {
				PathSegmentImpl graphSegment = (PathSegmentImpl) segment;
				NodeGraphFieldContainer container = (NodeGraphFieldContainer) graphSegment.getContainer();
				if (container == null || container.getElement() == null) {
					log.debug("Container or element of container is null. Segment is invalid.");
					return false;
				}
			} catch (RuntimeException e) {
				log.debug("Error while checking path segment {" + segment.getSegment() + "}", e);
				return false;
			}
		}
		return true;
	}

}
