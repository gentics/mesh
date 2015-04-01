package com.gentics.cailun.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.repository.generic.GenericTagRepository;

public interface TagRepository extends GenericTagRepository<Tag, GenericFile> {

	//TODO filter by name?
	@Query(value="MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag) where id(requestUser) = {0} and perm.`permissions-read` = true return tag",countQuery="MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(tag:Tag) where id(requestUser) = {0} and perm.`permissions-read` = true return count(tag)")
	public Page<Tag> findAll(User requestUser, Pageable pageable);

}
