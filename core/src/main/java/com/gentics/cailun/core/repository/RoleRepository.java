package com.gentics.cailun.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.auth.GraphPermission;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;

public interface RoleRepository extends GenericNodeRepository<Role> {

	Role findByName(String string);

	@Query("MATCH (role:Role)-[r:HAS_PERMISSION]->(node:GenericNode) WHERE id(node) = {1} AND id(role) = {0} return r")
	GraphPermission findPermission(Long roleId, Long nodeId);

	@Query(value = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleRole:Role) where id(requestUser) = {0} and perm.`permissions-read` = true return visibleRole ORDER BY visibleRole.name", countQuery = "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(visibleRole:Role) where id(requestUser) = {0} and perm.`permissions-read` = true return count(visibleRole)")
	Page<Role> findAll(User requestUser, Pageable pageable);

}
