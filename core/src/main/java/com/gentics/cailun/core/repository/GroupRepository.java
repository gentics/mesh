package com.gentics.cailun.core.repository;

import java.util.List;

import org.springframework.data.neo4j.annotation.Query;

import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.repository.generic.GenericNodeRepository;

public interface GroupRepository extends GenericNodeRepository<Group> {

	// @Query("MATCH (u:_User {0} ) MATCH (u)-[MEMBER_OF*]->(g) return g")

	/**
	 * Return all groups that are assigned to the user
	 * 
	 * @param user
	 * @return
	 */
	@Query("start u=node({0}) MATCH (u)-[MEMBER_OF*]->(g) return g")
	public List<Group> listAllGroups(User user);

	public Group findByName(String string);

}
