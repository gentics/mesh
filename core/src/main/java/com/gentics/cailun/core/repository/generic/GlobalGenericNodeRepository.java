package com.gentics.cailun.core.repository.generic;

import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.cailun.core.repository.action.GlobalI18NActions;
import com.gentics.cailun.core.repository.action.GlobalUUIDCRUDActions;
import com.gentics.cailun.core.rest.model.generic.GenericNode;

public interface GlobalGenericNodeRepository<T extends GenericNode> extends GraphRepository<T>, GlobalUUIDCRUDActions<T>, GlobalI18NActions<T> {

}
