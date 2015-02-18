package com.gentics.cailun.core.repository.project.generic;

import org.springframework.data.repository.Repository;

import com.gentics.cailun.core.repository.project.custom.ProjectUUIDCRUDActions;
import com.gentics.cailun.core.rest.model.generic.GenericNode;

public interface ProjectGenericNodeRepository<T extends GenericNode> extends ProjectUUIDCRUDActions<T>, Repository<T, Long> {

}
