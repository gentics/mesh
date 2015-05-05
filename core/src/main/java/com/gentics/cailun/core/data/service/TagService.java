package com.gentics.cailun.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;

import org.springframework.data.domain.Page;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.generic.GenericPropertyContainer;
import com.gentics.cailun.core.data.service.generic.GenericPropertyContainerService;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.paging.PagingInfo;

public interface TagService extends GenericPropertyContainerService<Tag> {

	TagResponse transformToRest(RoutingContext rc, Tag tag);

	/**
	 * Retrieve all visible tags in the given project.
	 * 
	 * @param rc
	 * @param projectName
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 */
	Page<Tag> findAllVisible(RoutingContext rc, String projectName, List<String> languageTags, PagingInfo pagingInfo);

	/**
	 * Retrieve all visible tags for the given tag in the given project.
	 * 
	 * @param rc
	 * @param projectName
	 * @param rootTag
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 */
	Page<Tag> findAllVisibleTags(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo);

	/**
	 * Retrieve all visible contents for the given tag in the given project.
	 * 
	 * @param rc
	 * @param projectName
	 * @param rootTag
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 */
	Page<Content> findAllVisibleContents(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo);

	/**
	 * Retrieve all visible child tags for the given tag.
	 * 
	 * @param rc
	 * @param projectName
	 * @param rootTag
	 * @param languageTags
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends GenericPropertyContainer> findAllVisibleChildNodes(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo);

}
