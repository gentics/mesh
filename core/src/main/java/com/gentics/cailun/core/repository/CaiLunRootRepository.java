package com.gentics.cailun.core.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.cailun.core.rest.model.CaiLunRoot;

public interface CaiLunRootRepository<T extends CaiLunRoot> extends GraphRepository<T> {

	@Query("MATCH (n:CaiLunRoot) return n")
	T findRoot();

}
