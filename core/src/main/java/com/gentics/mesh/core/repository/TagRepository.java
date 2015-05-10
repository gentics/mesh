package com.gentics.mesh.core.repository;

import com.gentics.mesh.core.data.model.Tag;
import com.gentics.mesh.core.repository.action.TagActions;
import com.gentics.mesh.core.repository.generic.GenericPropertyContainerRepository;

public interface TagRepository extends GenericPropertyContainerRepository<Tag>, TagActions {

}
