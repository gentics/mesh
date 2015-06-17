package com.gentics.mesh.core.data.model.tinkerpop;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_USER;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.rest.group.response.GroupResponse;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

public class Group extends GenericNode {

	public static final String NAME_KEY = "name";

	public String getName() {
		return getProperty(NAME_KEY);
	}

	public void setName(String name) {
		setProperty(NAME_KEY, name);
	}

	public Iterable<? extends MeshUser> getUsers() {
		return in(HAS_USER).toList(MeshUser.class);
	}

	//	@GremlinGroovy(value = "it.in('HAS_USER').order({ it.b.getProperty(fieldName) <=> it.a.getProperty(fieldName) })[skip..limit]")
	//	public Iterable<User> getUsersInOrder(@GremlinParam("fieldName") String fieldName, @GremlinParam("skip") int skip,
	//			@GremlinParam("limit") int limit) {
	//		in(HAS_USER).order()
	//	}

	// @Adjacency(label = HAS_USER, direction = Direction.IN)
	public void addUser(MeshUser user) {
		user.addFramedEdge(HAS_USER, this, MeshUser.class);
	}

	public void removeUser(MeshUser user) {
		unlinkIn(user, HAS_USER);
	}

	public List<? extends Role> getRoles() {
		return in(HAS_ROLE).toList(Role.class);
	}

	// @Adjacency(label = HAS_ROLE, direction = Direction.IN)
	public void addRole(Role role) {

	}

	public void removeRole(Role role) {
		unlinkIn(role, HAS_ROLE);
	}

	// TODO add java handler
	public boolean hasRole(Role extraRole) {
		//TODO this is not optimal - research a better way
		return in(HAS_ROLE).toList(Role.class).contains(extraRole);
	}

	// TODO add java handler
	public boolean hasUser(MeshUser extraUser) {
		//TODO this is not optimal - research a better way
		return in(HAS_USER).toList(Role.class).contains(extraUser);
	}

	/**
	 * Get all users within this group that are visible for the given user.
	 * 
	 * @param user
	 * @param pagingInfo
	 * @return
	 */
	public Page<MeshUser> getVisibleUsers(MeshShiroUser user, PagingInfo pagingInfo) {

		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) MATCH (user)-[:MEMBER_OF]-(group:Group) where id(group) = {1} AND requestUser.uuid = {0} and perm.`permissions-read` = true return user ORDER BY user.username",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) MATCH (user)-[:MEMBER_OF]-(group:Group) where id(group) = {1} AND requestUser.uuid = {0} and perm.`permissions-read` = true return count(user)")
		// Page<User> findByGroup(String userUuid, Group group, Pageable pageable);
		// return findByGroup(userUuid, group, new MeshPageRequest(pagingInfo));
		return null;
	}

	public Page<? extends Role> getRoles(MeshShiroUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {
		//		return findByGroup(userUuid, group, new MeshPageRequest(pagingInfo));
		//	@Query(value = MATCH_PERMISSION_ON_ROLE + " MATCH (role)-[:HAS_ROLE]->(group:Group) where id(group) = {1} AND " + FILTER_USER_PERM
		//			+ " return role ORDER BY role.name desc",

		//	countQuery = MATCH_PERMISSION_ON_ROLE + "MATCH (role)-[:HAS_ROLE]->(group:Group) where id(group) = {1} AND " + FILTER_USER_PERM
		//			+ "return count(role)")
		//		Page<Role> findByGroup(String userUuid, Group group, Pageable pageable) {
		//			return null;
		//		}
		VertexTraversal<?, ?, ?> traversal = out(HAS_ROLE);

		Page<? extends Role> page = TraversalHelper.getPagedResult(traversal, pagingInfo, Role.class);
		return page;

	}

	// TODO handle depth
	public GroupResponse transformToRest(MeshShiroUser user) {
		GroupResponse restGroup = new GroupResponse();
		restGroup.setUuid(getUuid());
		restGroup.setName(getName());

		// for (User user : group.getUsers()) {
		// user = neo4jTemplate.fetch(user);
		// String name = user.getUsername();
		// if (name != null) {
		// restGroup.getUsers().add(name);
		// }
		// }
		// Collections.sort(restGroup.getUsers());

		for (Role role : getRoles()) {
			String name = role.getName();
			if (name != null) {
				restGroup.getRoles().add(name);
			}
		}

		// // Set<Group> children = groupRepository.findChildren(group);
		// Set<Group> children = group.getGroups();
		// for (Group childGroup : children) {
		// restGroup.getGroups().add(childGroup.getName());
		// }

		return restGroup;

	}

	// @Override
	// public Group save(Group group) {
	// GroupRoot root = findRoot();
	// if (root == null) {
	// throw new NullPointerException("The group root node could not be found.");
	// }
	// group = neo4jTemplate.save(group);
	// root.getGroups().add(group);
	// neo4jTemplate.save(root);
	// return group;
	// return null;
	// }
}
