package com.gentics.cailun.core.repository;

import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.cailun.core.repository.action.GlobalI18NActions;
import com.gentics.cailun.core.repository.action.GlobalUUIDCRUDActions;
import com.gentics.cailun.core.rest.model.CaiLunNode;

public interface GlobalCaiLunNodeRepository<T extends CaiLunNode> extends GraphRepository<T>, GlobalUUIDCRUDActions<T>, GlobalI18NActions<T> {


}
