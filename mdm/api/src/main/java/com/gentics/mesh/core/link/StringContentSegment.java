package com.gentics.mesh.core.link;

import java.util.Optional;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.parameter.LinkType;

/**
 * Implementation of {@link ContentSegment} that represents a static string
 */
public class StringContentSegment implements ContentSegment {
	/**
	 * Static segment
	 */
	protected final String segment;

	/**
	 * Create an instance
	 * @param segment static segment
	 */
	public StringContentSegment(String segment) {
		this.segment = segment;
	}

	@Override
	public String render(WebRootLinkReplacer replacer, InternalActionContext ac, ContainerType edgeType, LinkType linkType, String projectName) {
		return segment;
	}

	@Override
	public Optional<String> getTargetUuid() {
		return Optional.empty();
	}

	@Override
	public Optional<String> getBranch() {
		return Optional.empty();
	}

	@Override
	public String toString() {
		return segment;
	}
}
