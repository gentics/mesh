package com.gentics.cailun.core.data.service;

import java.util.List;

import org.springframework.data.domain.Page;

import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.service.generic.GenericTagService;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.path.PagingInfo;
import com.gentics.cailun.path.Path;

public interface TagService extends GenericTagService<Tag, GenericFile> {

	Path findByProjectPath(String projectName, String path);

	TagResponse transformToRest(Tag tag, List<String> languages);

	Page<Tag> findAllVisible(User requestUser, String projectName, List<String> languageTags, PagingInfo pagingInfo);

}
