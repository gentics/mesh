package com.gentics.cailun.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.repository.RepositoryDefinition;

import com.gentics.cailun.core.data.model.auth.GraphPermission;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.RoleRoot;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.repository.action.RoleActions;
import com.gentics.cailun.core.repository.action.UUIDCRUDActions;

@RepositoryDefinition(domainClass = Role.class, idClass = Long.class)
public interface RoleRepository extends UUIDCRUDActions<Role>, RoleActions {

	Role findByName(String string);

	Role findOne(Long id);

	Result<Role> findAll();

	@Query("MATCH (role:Role)-[r:HAS_PERMISSION]->(node) WHERE id(node) = {1} AND id(role) = {0} return r")
	GraphPermission findPermission(Long roleId, Long nodeId);

	@Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleRole:Role) where id(requestUser) = {0} and perm.`permissions-read` = true return visibleRole ORDER BY visibleRole.name", countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleRole:Role) where id(requestUser) = {0} and perm.`permissions-read` = true return count(visibleRole)")
	Page<Role> findAll(User requestUser, Pageable pageable);

	@Query("MATCH (n:RoleRoot) return n")
	RoleRoot findRoot();

}
