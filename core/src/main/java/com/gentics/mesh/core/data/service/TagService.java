package com.gentics.mesh.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;

import org.springframework.data.domain.Page;

import com.gentics.mesh.core.data.model.Content;
import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.core.data.service.generic.GenericPropertyContainerService;
import com.gentics.mesh.core.rest.tag.response.TagResponse;
import com.gentics.mesh.paging.PagingInfo;

public interface TagService extends GenericPropertyContainerService<Tag> {

	TagResponse transformToRest(RoutingContext rc, Tag tag);

	Page<Tag> findAllVisibleTags(RoutingContext rc, String projectName, List<String> languageTags, PagingInfo pagingInfo);

	/**
	 * Find all tags that tag the given root tag. (rootTag:Tag)-[:HAS_TAG]->(tag:Tag)
	 * 
	 * @param rc
	 * @param projectName
	 * @param rootTag
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 */
	Page<Tag> findTaggedTags(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo);

	/**
	 * Find all tags that are tagged by the given tag. (rootTag:Tag)<-[:HAS_TAG]-(tag:Tag)
	 * 
	 * @param rc
	 * @param projectName
	 * @param rootTag
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 */
	Page<Tag> findTaggingTags(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo);

	/**
	 * Find all child contents for the given tag.
	 * 
	 * @param rc
	 * @param projectName
	 * @param rootTag
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 */
	Page<Content> findChildContents(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo);

	/**
	 * Find all child tags for the given tag.
	 * 
	 * @param rc
	 * @param projectName
	 * @param rootTag
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 */
	Page<Tag> findChildTags(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo);

	/**
	 * Find all contents that are tagged by the given tag.
	 * 
	 * @param rc
	 * @param projectName
	 * @param rootTag
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 */
	Page<Content> findTaggedContents(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo);

}
