package com.gentics.cailun.core.repository;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.cailun.core.rest.model.Tag;

public interface TagRepository extends GraphRepository<Tag> {

	@Query("MATCH (tag:Tag) RETURN tag")
	public List<Tag> findAllTags();
	
	@Query("MATCH (tag:Tag {name: '/'}) RETURN tag")
	public Tag findRootTag();
	
}
