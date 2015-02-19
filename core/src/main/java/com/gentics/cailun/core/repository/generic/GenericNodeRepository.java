package com.gentics.cailun.core.repository.generic;

import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.cailun.core.repository.action.I18NActions;
import com.gentics.cailun.core.repository.action.UUIDCRUDActions;
import com.gentics.cailun.core.rest.model.generic.GenericNode;

public interface GenericNodeRepository<T extends GenericNode> extends GraphRepository<T>, UUIDCRUDActions<T>, I18NActions<T> {

}
