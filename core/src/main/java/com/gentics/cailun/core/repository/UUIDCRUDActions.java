package com.gentics.cailun.core.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.gentics.cailun.core.rest.model.CaiLunNode;

@NoRepositoryBean
public interface UUIDCRUDActions<T extends CaiLunNode> extends GraphRepository<T> {

	@Query("MATCH (content:Content) WHERE content.uuid = {0} return content")
	public T findByUUID(String uuid);

	@Query("MATCH (n:Content {uuid: {0}} DELETE n")
	public void delete(String uuid);

}
