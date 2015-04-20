package com.gentics.cailun.core.data.service;

import io.vertx.ext.apex.RoutingContext;

import java.util.List;

import org.springframework.data.domain.Page;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.generic.GenericPropertyContainerService;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.path.PagingInfo;
import com.gentics.cailun.path.Path;

public interface TagService extends GenericPropertyContainerService<Tag> {

	Path findByProjectPath(String projectName, String path);

	TagResponse transformToRest(RoutingContext rc, Tag tag, List<String> languages, int depth);

	Page<Tag> findAllVisible(User requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo);

	Page<Tag> findAllVisibleSubTags(User requestUser, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo);

	Page<Content> findAllVisibleSubContents(User requestUser, String projectName, Tag rootTag, List<String> languageTags, PagingInfo pagingInfo);

}
