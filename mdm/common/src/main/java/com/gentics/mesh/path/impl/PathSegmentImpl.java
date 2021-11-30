package com.gentics.mesh.path.impl;

import com.gentics.mesh.core.data.HibField;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.path.PathSegment;

/**
 * A webroot path is built using multiple path segments. Each node is able to provide multiple path segments. Each language of the node may provide different
 * path segments. The path segment will store a reference to the found container.
 */
public class PathSegmentImpl implements PathSegment {

	private HibNodeFieldContainer container;
	private HibField pathField;
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
	public PathSegmentImpl(HibNodeFieldContainer container, HibField pathField, String languageTag, String segment) {
		this.container = container;
		this.pathField = pathField;
		this.languageTag = languageTag;
		this.segment = segment;
	}

	@Override
	public HibNodeFieldContainer getContainer() {
		return container;
	}

	/**
	 * Return the graph field which provides this segment for the node.
	 * 
	 * @return
	 */
	public HibField getPathField() {
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
