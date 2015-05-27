package com.gentics.mesh.core.repository;

import static com.gentics.mesh.core.repository.CypherStatements.FILTER_USER_PERM;
import static com.gentics.mesh.core.repository.CypherStatements.MATCH_PERMISSION_ON_ROLE;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.repository.RepositoryDefinition;

import com.gentics.mesh.core.data.model.auth.GraphPermission;
import com.gentics.mesh.core.data.model.auth.Group;
import com.gentics.mesh.core.data.model.auth.Role;
import com.gentics.mesh.core.data.model.auth.RoleRoot;
import com.gentics.mesh.core.repository.action.RoleActions;
import com.gentics.mesh.core.repository.action.UUIDCRUDActions;

@RepositoryDefinition(domainClass = Role.class, idClass = Long.class)
public interface RoleRepository extends UUIDCRUDActions<Role>, RoleActions {

	Role findByName(String string);

	Role findOne(Long id);

	Result<Role> findAll();

	@Query("MATCH (role:Role)-[r:HAS_PERMISSION]->(node) WHERE id(node) = {1} AND id(role) = {0} return r")
	GraphPermission findPermission(Long roleId, Long nodeId);

	@Query(value = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + "return role ORDER BY role.name",

	countQuery = MATCH_PERMISSION_ON_ROLE + " WHERE " + FILTER_USER_PERM + " return count(role)")
	Page<Role> findAll(String userUuid, Pageable pageable);

	@Query("MATCH (n:RoleRoot) return n")
	RoleRoot findRoot();

	@Query(value = MATCH_PERMISSION_ON_ROLE + " MATCH (role)-[:HAS_ROLE]->(group:Group) where id(group) = {1} AND " + FILTER_USER_PERM
			+ " return role ORDER BY role.name desc",

	countQuery = MATCH_PERMISSION_ON_ROLE + "MATCH (role)-[:HAS_ROLE]->(group:Group) where id(group) = {1} AND " + FILTER_USER_PERM
			+ "return count(role)")
	Page<Role> findByGroup(String userUuid, Group group, Pageable pageable);

}
