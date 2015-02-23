package com.gentics.cailun.core.repository.generic;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.cailun.core.repository.action.I18NActions;
import com.gentics.cailun.core.repository.action.UUIDCRUDActions;
import com.gentics.cailun.core.rest.model.generic.GenericNode;

public interface GenericNodeRepository<T extends GenericNode> extends GraphRepository<T>, UUIDCRUDActions<T>, I18NActions<T> {

	@Query("MATCH (n:GenericNode)-[:ASSIGNED_TO_PROJECT]-(p:Project) WHERE p.name = {0} return n")
	Result<T> findAll(String projectName);

}
