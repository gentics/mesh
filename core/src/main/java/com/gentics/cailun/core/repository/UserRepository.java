package com.gentics.cailun.core.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.auth.UserRoot;
import com.gentics.cailun.core.repository.action.UserActions;

public interface UserRepository extends GraphRepository<User>, UserActions {

	User findByFirstnameEquals(String firstname);

	User findByUsername(String username);

	@Query("MATCH (u:_User) WHERE u.username + '%' + u.emailAddress + '%' +  u.passwordHash = {0} return u")
	User findByPrincipalId(String principalId);
	
	/**
	 * Returns a Page of users meeting the paging restriction provided in the Pageable object.
	 * 
	 * @param requestUser
	 * @param pageable
	 * @return
	 */
	@Query(value="MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) where id(requestUser) = {0} and perm.`permissions-read` = true return user ORDER BY user.username",countQuery="MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) where id(requestUser) = {0} and perm.`permissions-read` = true return count(user)")
	public Page<User> findAll(User requestUser, Pageable pageable);
	
	@Query("MATCH (n:UserRoot) return n")
	UserRoot findRoot();
	

}
