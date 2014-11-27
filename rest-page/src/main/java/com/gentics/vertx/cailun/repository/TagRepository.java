package com.gentics.vertx.cailun.repository;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

public interface TagRepository extends GraphRepository<Tag> {

	@Query("MATCH (tag:Tag) RETURN tag")
	public List<Tag> findAllTags();
}
