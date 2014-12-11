package com.gentics.vertx.cailun.repository;

import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.vertx.cailun.model.perm.User;

public interface UserRepository extends GraphRepository<User> {

}
