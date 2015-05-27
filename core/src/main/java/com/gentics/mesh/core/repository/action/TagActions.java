package com.gentics.mesh.core.repository.action;

import java.util.List;

import org.springframework.data.domain.Page;

import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.paging.PagingInfo;

public interface TagActions {

	/**
	 * Find all visible tags within the given project. /tags
	 * 
	 * @param userUuid
	 * @param projectName
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 */
	public Page<Tag> findProjectTags(String userUuid, String projectName, List<String> languageTags, PagingInfo pagingInfo);

	/**
	 * Find all tags that tag the given node. /nodes/:uuid/tags
	 * 
	 * @param userUuid
	 * @param projectName
	 * @param node
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 */
	Page<Tag> findTags(String userUuid, String projectName, MeshNode node, List<String> languageTags, PagingInfo pagingInfo);

	/**
	 * Find all visible nodes that use this tag for tagging. /tags/:uuid/nodes
	 * 
	 * @param userUuid
	 * @param projectName
	 * @param rootTag
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 */
	Page<MeshNode> findTaggedNodes(String userUuid, String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo);

}
