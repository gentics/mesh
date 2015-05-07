package com.gentics.mesh.core.repository.generic;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.repository.action.I18NActions;
import com.gentics.mesh.core.repository.action.UUIDCRUDActions;

public interface GenericNodeRepository<T extends GenericNode> extends GraphRepository<T>, UUIDCRUDActions<T>, I18NActions<T> {

	@Query("MATCH (n:GenericNode)-[:ASSIGNED_TO_PROJECT]-(p:Project) WHERE p.name = {0} return n")
	Iterable<T> findAll(String projectName);

}
