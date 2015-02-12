package com.gentics.cailun.core.repository.project;

import org.springframework.data.repository.Repository;

import com.gentics.cailun.core.repository.project.custom.ProjectUUIDCRUDActions;
import com.gentics.cailun.core.rest.model.CaiLunNode;

public interface ProjectCaiLunNodeRepository<T extends CaiLunNode> extends ProjectUUIDCRUDActions<T>, Repository<T, Long> {

}
