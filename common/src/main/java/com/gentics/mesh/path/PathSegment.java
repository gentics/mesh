package com.gentics.mesh.path;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.field.GraphField;

/**
 * A webroot path is built using multiple path segments. Each node is able to provide multiple path segments. Each language of the node may provide different
 * path segments. The path segment will store a reference to the found container.
 */
public class PathSegment {

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
	public PathSegment(NodeGraphFieldContainer container, GraphField pathField, String languageTag, String segment) {
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

	/**
	 * Return the language tag of the node which provides this segment.
	 * 
	 * @return
	 */
	public String getLanguageTag() {
		return languageTag;
	}

	/**
	 * Return the segment which was used to resolve the path segment.
	 * 
	 * @return
	 */
	public String getSegment() {
		return segment;
	}

}
