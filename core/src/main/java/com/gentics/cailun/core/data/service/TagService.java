package com.gentics.cailun.core.data.service;

import java.util.List;

import com.gentics.cailun.core.data.model.Language;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.service.generic.GenericTagService;
import com.gentics.cailun.core.rest.tag.response.TagResponse;

public interface TagService extends GenericTagService<Tag, GenericFile> {

	Tag findByProjectPath(String projectName, String path);

	TagResponse transformToRest(Tag tag, List<String> languages);

}
