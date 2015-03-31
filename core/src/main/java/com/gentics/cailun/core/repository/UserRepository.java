package com.gentics.cailun.core.repository;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.repository.action.UserPagingActions;

public interface UserRepository extends GraphRepository<User>, UserPagingActions {

	User findByFirstnameEquals(String firstname);

	User findByUsername(String username);

	@Query("MATCH (u:_User) WHERE u.username + '%' + u.emailAddress + '%' +  u.passwordHash = {0} return u")
	User findByPrincipalId(String principalId);

}
