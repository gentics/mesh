package com.gentics.vertx.cailun.perm;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.vertx.cailun.perm.model.User;

public interface UserRepository extends GraphRepository<User> {

	User findByFirstnameEquals(String firstname);

	User findByUsername(String username);
	
	@Query("MATCH (u:_User) WHERE u.username + '%' + u.emailAddress + '%' +  u.passwordHash = {0} return u")
	User findByPrincipalId(String principalId);
	
}
