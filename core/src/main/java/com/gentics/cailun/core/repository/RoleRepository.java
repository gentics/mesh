package com.gentics.cailun.core.repository;

import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.cailun.core.rest.model.Role;

public interface RoleRepository extends GraphRepository<Role> {

}
