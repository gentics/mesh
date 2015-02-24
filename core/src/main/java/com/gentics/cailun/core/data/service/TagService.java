package com.gentics.cailun.core.data.service;

import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.service.generic.GenericTagService;

public interface TagService extends GenericTagService<Tag, GenericFile> {

	Tag findByProjectPath(String projectName, String path);

}
