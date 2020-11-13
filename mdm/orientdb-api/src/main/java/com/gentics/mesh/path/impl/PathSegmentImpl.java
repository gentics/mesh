package com.gentics.mesh.path.impl;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.path.PathSegment;

/**
 * A webroot path is built using multiple path segments. Each node is able to provide multiple path segments. Each language of the node may provide different
 * path segments. The path segment will store a reference to the found container.
 */
public class PathSegmentImpl implements PathSegment {

	private NodeGraphFieldContainer container;
	private GraphField pathField;
	private String languageTag;
	private String segment;

	/**
	 * Create a new path segment.
	 * 
	 * @param container
	 *            Container that provides this segment
	 * @param pathField
	 *            Graph field which is the source of the segment
	 * @param languageTag
	 *            Language of the segment field
	 */
	public PathSegmentImpl(NodeGraphFieldContainer container, GraphField pathField, String languageTag, String segment) {
		this.container = container;
		this.pathField = pathField;
		this.languageTag = languageTag;
		this.segment = segment;
	}

	/**
	 * Return the container for the segment.
	 * 
	 * @return
	 */
	public NodeGraphFieldContainer getContainer() {
		return container;
	}

	/**
	 * Return the graph field which provides this segment for the node.
	 * 
	 * @return
	 */
	public GraphField getPathField() {
		return pathField;
	}

	@Override
	public String getLanguageTag() {
		return languageTag;
	}

	@Override
	public String getSegment() {
		return segment;
	}

}
