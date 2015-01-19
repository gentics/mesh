package com.gentics.vertx.cailun.tag;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.vertx.cailun.tag.model.Tag;

public interface TagRepository extends GraphRepository<Tag> {

	@Query("MATCH (tag:Tag) RETURN tag")
	public List<Tag> findAllTags();
	
	@Query("MATCH (tag:Tag {name: '/'}) RETURN tag")
	public Tag findRootTag();
	
}
