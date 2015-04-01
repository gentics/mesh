package com.gentics.cailun.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.repository.generic.GenericContentRepository;

public interface ContentRepository extends GenericContentRepository<Content> {

	@Query("MATCH (n:Content)-[:ASSIGNED_TO_PROJECT]-(p:Project) WHERE p.name = {0} return n")
	Iterable<Content> findAll(String projectName);

	//TODO order content by?
	@Query(value="MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(content:Content) where id(requestUser) = {0} and perm.`permissions-read` = true return content",countQuery="MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(content:Content) where id(requestUser) = {0} and perm.`permissions-read` = true return count(content)")
	Page<Content> findAll(User requestUser, Pageable pageable);
}
