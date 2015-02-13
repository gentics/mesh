package com.gentics.cailun.core.repository;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.rest.model.LocalizedTag;

public interface GlobalLocalizedTagRepository<T extends LocalizedTag> extends GlobalLocalizedNodeRepository<T> {

	@Query("MATCH (tag:Tag) RETURN tag")
	public List<T> findAllTags();

	@Query("MATCH (tag:Tag {name: '/'}) RETURN tag")
	public T findRootTag();

}
