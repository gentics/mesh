package com.gentics.cailun.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;

import org.springframework.data.domain.Page;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.service.generic.GenericPropertyContainerService;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.paging.PagingInfo;

public interface TagService extends GenericPropertyContainerService<Tag> {

	TagResponse transformToRest(RoutingContext rc, Tag tag, List<String> languages, int depth);

	Page<Tag> findAllVisible(RoutingContext rc, String projectName, List<String> languageTags, PagingInfo pagingInfo);

	Page<Tag> findAllVisibleSubTags(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo);

	Page<Content> findAllVisibleSubContents(RoutingContext rc, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo);

}
