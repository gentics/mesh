package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;

import org.springframework.data.domain.Page;

import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.core.data.service.generic.GenericPropertyContainerService;
import com.gentics.mesh.core.rest.tag.response.TagResponse;
import com.gentics.mesh.paging.PagingInfo;

public interface TagService extends GenericPropertyContainerService<Tag> {

	TagResponse transformToRest(RoutingContext rc, Tag tag);

	/**
	 * Find all tags that were assigned to the given project
	 * 
	 * @param rc
	 * @param projectName
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 */
	Page<Tag> findProjectTags(RoutingContext rc, String projectName, List<String> languageTags, PagingInfo pagingInfo);

	/**
	 * Find all tags for the given node.
	 * 
	 * @param rc
	 * @param projectName
	 * @param node
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 */
	Page<Tag> findTags(RoutingContext rc, String projectName, MeshNode node, List<String> languageTags, PagingInfo pagingInfo);

	/**
	 * Find all nodes that are tagged by the given tag.
	 * 
	 * @param rc
	 * @param projectName
	 * @param tag
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 */
	Page<MeshNode> findTaggedNodes(RoutingContext rc, String projectName, Tag tag, List<String> languageTags, PagingInfo pagingInfo);

}
