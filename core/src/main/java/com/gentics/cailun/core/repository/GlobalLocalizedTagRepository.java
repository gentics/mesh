package com.gentics.cailun.core.repository;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.rest.model.LocalizedTag;

public interface GlobalLocalizedTagRepository extends GlobalTaggableNodeRepository<LocalizedTag> {

	@Query("MATCH (tag:Tag) RETURN tag")
	public List<LocalizedTag> findAllTags();

	@Query("MATCH (tag:Tag {name: '/'}) RETURN tag")
	public LocalizedTag findRootTag();

}
