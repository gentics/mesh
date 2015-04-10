package com.gentics.cailun.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.repository.RepositoryDefinition;

import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.ProjectRoot;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.repository.action.ProjectActions;
import com.gentics.cailun.core.repository.action.UUIDCRUDActions;

@RepositoryDefinition(domainClass = Project.class, idClass = Long.class)
public interface ProjectRepository extends UUIDCRUDActions<Project>, ProjectActions {

	Project findByName(String string);

	void deleteByName(String name);

	@Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(project:Project) where id(requestUser) = {0} and perm.`permissions-read` = true return project ORDER BY project.name", countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(project:Project) where id(requestUser) = {0} and perm.`permissions-read` = true return count(project)")
	public Page<Project> findAll(User requestUser, Pageable pageable);

	public Content findFileByPath(String projectName, String path);

	@Query("MATCH (n:ProjectRoot) return n")
	ProjectRoot findRoot();

	Result<Project> findAll();

}
