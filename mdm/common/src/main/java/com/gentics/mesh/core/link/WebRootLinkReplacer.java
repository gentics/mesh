package com.gentics.mesh.core.link;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.parameter.LinkType;

public interface WebRootLinkReplacer {

	/**
	 * Resolve the link to the node with uuid (in the given language) into an observable
	 * 
	 * @param ac
	 * @param branch
	 *            branch Uuid or name
	 * @param edgeType
	 *            edge type
	 * @param uuid
	 *            target uuid
	 * @param type
	 *            link type
	 * @param projectName
	 *            project name (which is used for 404 links)
	 * @param languageTags
	 *            optional language tags
	 * @return observable of the rendered link
	 */
	String resolve(InternalActionContext ac, String branch, ContainerType edgeType, String uuid, LinkType type, String projectName,
		String... languageTags);

	/**
	 * Resolve the link to the node with uuid (in the given language) into an observable
	 * 
	 * @param ac
	 * @param branch
	 *            branch Uuid or name
	 * @param edgeType
	 *            edge type
	 * @param uuid
	 *            target uuid
	 * @param type
	 *            link type
	 * @param projectName
	 *            project name (which is used for 404 links)
	 * @param forceAbsolute
	 * 			  if true, the resolved link will always be absolute
	 * @param languageTags
	 *            optional language tags
	 * @return observable of the rendered link
	 */
	String resolve(InternalActionContext ac, String branch, ContainerType edgeType, String uuid, LinkType type, String projectName,
		boolean forceAbsolute, String... languageTags);

	/**
	 * Resolve the link to the given node.
	 * 
	 * @param ac
	 * @param branchNameOrUuid
	 *            Branch UUID or name which will be used to render the path to the linked node. If this is invalid, the default branch of the target node will
	 *            be used.
	 * @param edgeType
	 *            edge type
	 * @param node
	 *            target node
	 * @param type
	 *            link type
	 * @param languageTags
	 *            target language
	 * @return observable of the rendered link
	 */
	String resolve(InternalActionContext ac, String branchNameOrUuid, ContainerType edgeType, HibNode node, LinkType type, String... languageTags);

	/**
	 * Resolve the link to the given node.
	 * 
	 * @param ac
	 * @param branchNameOrUuid
	 *            Branch UUID or name which will be used to render the path to the linked node. If this is invalid, the default branch of the target node will
	 *            be used.
	 * @param edgeType
	 *            edge type
	 * @param node
	 *            target node
	 * @param type
	 *            link type
	 * @param forceAbsolute
	 * 			  if true, the resolved link will always be absolute
	 * @param languageTags
	 *            target language
	 * @return observable of the rendered link
	 */
	String resolve(InternalActionContext ac, String branchNameOrUuid, ContainerType edgeType, HibNode node, LinkType type, boolean forceAbsolute,
		String... languageTags);

	/**
	 * Replace the links in the content.
	 * 
	 * @param ac
	 * @param branch
	 *            branch Uuid or name
	 * @param edgeType
	 *            edge type
	 * @param content
	 *            content containing links to replace
	 * @param type
	 *            replacing type
	 * @param projectName
	 *            project name (used for 404 links)
	 * @param languageTags
	 *            optional language tags
	 * @return content with links (probably) replaced
	 */
	String replace(InternalActionContext ac, String branch, ContainerType edgeType, String content, LinkType type, String projectName,
		List<String> languageTags);

}
