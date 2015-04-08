package com.gentics.cailun.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;

public interface ProjectRepository extends GenericNodeRepository<Project> {

	Project findByName(String string);

	void deleteByName(String name);

	@Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(project:Project) where id(requestUser) = {0} and perm.`permissions-read` = true return project ORDER BY project.name", countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(project:Project) where id(requestUser) = {0} and perm.`permissions-read` = true return count(project)")
	public Page<Project> findAll(User requestUser, Pageable pageable);

	public Content findFileByPath(String projectName, String path);

}
