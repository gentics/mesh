package com.gentics.cailun.core.repository;

import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.auth.GraphPermission;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;

public interface RoleRepository extends GenericNodeRepository<Role> {

	Role findByName(String string);

	@Query("MATCH (role:Role)-[r:HAS_PERMISSION]->(node:GenericNode) WHERE id(node) = {1} AND id(role) = {0} return r")
	GraphPermission findPermission(Long roleId, Long nodeId);

}
