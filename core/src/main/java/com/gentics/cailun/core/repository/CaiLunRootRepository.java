package com.gentics.cailun.core.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.repository.Repository;

import com.gentics.cailun.core.data.model.CaiLunRoot;

public interface CaiLunRootRepository extends Repository<CaiLunRoot, Long> {

	@Query("MATCH (n:CaiLunRoot) return n")
	CaiLunRoot findRoot();

	void save(CaiLunRoot rootNode);

	CaiLunRoot findOne(Long id);

}
