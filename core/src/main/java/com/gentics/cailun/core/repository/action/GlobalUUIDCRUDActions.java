package com.gentics.cailun.core.repository.action;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.gentics.cailun.core.rest.model.generic.GenericNode;

@NoRepositoryBean
public interface GlobalUUIDCRUDActions<T extends GenericNode> extends GraphRepository<T> {

	@Query("MATCH (content:Content) WHERE content.uuid = {0} return content")
	public T findByUUID(String uuid);

	@Query("MATCH (n:Content {uuid: {0}} DELETE n")
	public void delete(String uuid);

}
