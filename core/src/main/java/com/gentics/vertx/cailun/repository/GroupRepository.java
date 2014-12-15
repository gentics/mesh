package com.gentics.vertx.cailun.repository;

import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.vertx.cailun.model.perm.Group;

public interface GroupRepository extends GraphRepository<Group> {

}
