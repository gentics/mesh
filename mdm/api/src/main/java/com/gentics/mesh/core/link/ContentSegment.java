package com.gentics.mesh.core.link;

import java.util.Optional;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.parameter.LinkType;

/**
 * Interface for segments in content, the possibly contain links (in the form of <code>{{mesh.link(uuid, language, branch)}}</code>
 */
public interface ContentSegment {
	/**
	 * Render the content segment (optionally replacing the links)
	 * @param replacer link replacer
	 * @param ac action context
	 * @param edgeType edge type
	 * @param linkType link type
	 * @param projectName project name
	 * @return rendered segment
	 */
	String render(WebRootLinkReplacer replacer, InternalActionContext ac, ContainerType edgeType, LinkType linkType,
			String projectName);

	/**
	 * Get the optional target UUID (if the segment is a link)
	 * @return optional target UUID
	 */
	Optional<String> getTargetUuid();

	/**
	 * Get the optional language tags (if the segment is a link)
	 * @return optional language tags
	 */
	Optional<String[]> getLanguageTags();

	/**
	 * Get the optional branch (if the segment is a link specifying a branch)
	 * @return optional branch
	 */
	Optional<String> getBranch();
}
