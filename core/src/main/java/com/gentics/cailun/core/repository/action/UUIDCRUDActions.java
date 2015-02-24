package com.gentics.cailun.core.repository.action;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.NoRepositoryBean;

import com.gentics.cailun.core.data.model.generic.GenericNode;

@NoRepositoryBean
public interface UUIDCRUDActions<T extends GenericNode> {

	@Query("MATCH (content:GenericNode) WHERE content.uuid = {0} return content")
	public T findByUUID(String uuid);

	@Query("MATCH (n:Content {uuid: {0}} DELETE n")
	public void delete(String uuid);

	// T findCustomerNodeBySomeStrangeCriteria(Object strangeCriteria);

	@Query("MATCH (project:Project)<-[:ASSIGNED_TO_PROJECT]-(n:GenericNode)-[:HAS_I18N_PROPERTIES]->(p:I18NProperties) WHERE p.`properties-name` = {1} AND project.name = {0} RETURN n")
	public T findByName(String project, String name);

	@Query("MATCH (n:GenericNode)-[:ASSIGNED_TO_PROJECT]-(p:Project) WHERE n.uuid = {1} and p.name = {0} return n")
	public T findByUUID(String project, String uuid);

}
