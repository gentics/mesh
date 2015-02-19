package com.gentics.cailun.core.repository;

import com.gentics.cailun.core.repository.generic.GenericTagRepository;
import com.gentics.cailun.core.rest.model.Tag;
import com.gentics.cailun.core.rest.model.generic.GenericFile;

public interface TagRepository extends GenericTagRepository<Tag, GenericFile> {

}
