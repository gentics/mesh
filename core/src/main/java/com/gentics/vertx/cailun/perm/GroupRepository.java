package com.gentics.vertx.cailun.perm;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;
import org.springframework.data.neo4j.repository.GraphRepository;

import com.gentics.vertx.cailun.perm.model.Group;
import com.gentics.vertx.cailun.perm.model.User;

public interface GroupRepository extends GraphRepository<Group> {

	// @Query("MATCH (u:_User {0} ) MATCH (u)-[MEMBER_OF*]->(g) return g")

	/**
	 * Return all groups that are assigned to the user
	 * @param user
	 * @return
	 */
	@Query("start u=node({0}) MATCH (u)-[MEMBER_OF*]->(g) return g")
	public List<Group> listAllGroups(User user);

}
