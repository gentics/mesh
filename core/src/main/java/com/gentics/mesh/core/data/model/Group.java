package com.gentics.mesh.core.data.model;

import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.model.relationship.MeshRelationships.HAS_USER;

import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.generic.GenericNode;
import com.gentics.mesh.core.data.model.relationship.Permission;
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

	public List<? extends MeshUser> getUsers() {
		return in(HAS_USER).has(MeshUser.class).toListExplicit(MeshUser.class);
	}

	public void addUser(MeshUser user) {
		//TODO use link method
		user.addFramedEdge(HAS_USER, this, MeshUser.class);
	}

	public void removeUser(MeshUser user) {
		unlinkIn(user, HAS_USER);
	}

	public List<? extends Role> getRoles() {
		return in(HAS_ROLE).toList(Role.class);
	}

	public void addRole(Role role) {
		linkIn(role, HAS_ROLE);
	}

	public void removeRole(Role role) {
		unlinkIn(role, HAS_ROLE);
	}

	// TODO add java handler
	public boolean hasRole(Role extraRole) {
		// TODO this is not optimal - research a better way
		return in(HAS_ROLE).toList(Role.class).contains(extraRole);
	}

	// TODO add java handler
	public boolean hasUser(MeshUser extraUser) {
		// TODO this is not optimal - research a better way
		return in(HAS_USER).toList(Role.class).contains(extraUser);
	}

	/**
	 * Get all users within this group that are visible for the given user.
	 */
	public Page<? extends MeshUser> getVisibleUsers(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {

		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) MATCH (user)-[:MEMBER_OF]-(group:Group) where id(group) = {1} AND requestUser.uuid = {0} and perm.`permissions-read` = true return user ORDER BY user.username",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) MATCH (user)-[:MEMBER_OF]-(group:Group) where id(group) = {1} AND requestUser.uuid = {0} and perm.`permissions-read` = true return count(user)")
		// Page<User> findByGroup(String userUuid, Group group, Pageable pageable);
		// return findByGroup(userUuid, group, new MeshPageRequest(pagingInfo));

		// VertexTraversal traversal = requestUser.in(HAS_USER).out(HAS_ROLE).out(Permission.READ_PERM.getLabel()).has(MeshUser.class);
		VertexTraversal<?, ?, ?> traversal = requestUser.in(HAS_USER).out(HAS_ROLE).out(Permission.READ_PERM.label()).has(MeshUser.class);
		VertexTraversal<?, ?, ?> countTraversal = requestUser.in(HAS_USER).out(HAS_ROLE).out(Permission.READ_PERM.label()).has(MeshUser.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, MeshUser.class);
	}

	public Page<? extends Role> getRoles(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {

		VertexTraversal<?, ?, ?> traversal = in(HAS_ROLE);
		VertexTraversal<?, ?, ?> countTraversal = in(HAS_ROLE);

		Page<? extends Role> page = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, Role.class);
		return page;

	}

	// TODO handle depth
	public GroupResponse transformToRest(MeshAuthUser user) {
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

	public MeshUser createUser(String username) {
		MeshUser user = getGraph().addFramedVertex(MeshUser.class);
		//TODO also add user to userroot
		addUser(user);
		return user;
	}

	public Role createRole(String name) {
		Role role = getGraph().addFramedVertex(Role.class);
		// Add role also to role root
		addRole(role);
		return role;
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
