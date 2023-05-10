package com.gentics.mesh.core.link;

import java.util.Optional;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.parameter.LinkType;

/**
 * Implementation of {@link ContentSegment} that represents a link
 */
public class LinkContentSegment implements ContentSegment {
	/**
	 * Target UUID
	 */
	protected final String targetUuid;

	/**
	 * Optional branch
	 */
	protected final String branch;

	/**
	 * Optional language tags
	 */
	protected final String[] languageTags;

	/**
	 * Create an instance
	 * @param targetUuid target UUID
	 * @param branch optional branch
	 * @param languageTags optional language tags
	 */
	public LinkContentSegment(String targetUuid, String branch, String... languageTags) {
		this.targetUuid = targetUuid;
		this.branch = branch;
		this.languageTags = languageTags;
	}

	@Override
	public String render(WebRootLinkReplacer replacer, InternalActionContext ac, ContainerType edgeType, LinkType linkType, String projectName) {
		return replacer.resolve(ac, branch, edgeType, targetUuid, linkType, projectName, languageTags);
	}

	@Override
	public Optional<String> getTargetUuid() {
		return Optional.ofNullable(targetUuid);
	}

	@Override
	public Optional<String> getBranch() {
		return Optional.ofNullable(branch);
	}

	@Override
	public String toString() {
		return "";
	}
}
