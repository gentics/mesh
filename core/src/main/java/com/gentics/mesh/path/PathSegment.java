package com.gentics.mesh.path;

import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.node.Node;

/**
 * A webroot path is build using multiple path segments. Each node is able to provide multiple path segments. Each language of the node may provide different
 * path segments. The binary field also provides a path segment.
 */
public class PathSegment {

	private Node node;

	private Language language;

	private boolean binarySegment;

	public PathSegment(Node node, boolean binarySegment, Language language) {
		this.node = node;
		this.binarySegment = binarySegment;
		this.language = language;
	}

	/**
	 * Return the node for the segment.
	 * 
	 * @return
	 */
	public Node getNode() {
		return node;
	}

	/**
	 * Check whether the segment is a binary segment.
	 * 
	 * @return
	 */
	public boolean isBinarySegment() {
		return binarySegment;
	}

	/**
	 * Return the language of the node which provides this segment.
	 * 
	 * @return
	 */
	public Language getLanguage() {
		return language;
	}

}
