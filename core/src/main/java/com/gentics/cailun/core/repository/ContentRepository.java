package com.gentics.cailun.core.repository;

import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.repository.generic.GenericContentRepository;

public interface ContentRepository extends GenericContentRepository<Content> {

	@Query("MATCH (n:Content)-[:ASSIGNED_TO_PROJECT]-(p:Project) WHERE p.name = {0} return n")
	Iterable<Content> findAll(String projectName);
}
