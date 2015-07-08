package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.MeshRelationships.HAS_USER;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractGenericNode;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

public class GroupImpl extends AbstractGenericNode implements Group {

	public static final String NAME_KEY = "name";

	public String getName() {
		return getProperty(NAME_KEY);
	}

	public void setName(String name) {
		setProperty(NAME_KEY, name);
	}

	public List<? extends User> getUsers() {
		return in(HAS_USER).has(UserImpl.class).toListExplicit(UserImpl.class);
	}

	public void addUser(User user) {
		// TODO use link method
		user.getImpl().addFramedEdge(HAS_USER, this, UserImpl.class);
	}

	public void removeUser(User user) {
		unlinkIn(user.getImpl(), HAS_USER);
	}

	public List<? extends Role> getRoles() {
		return in(HAS_ROLE).has(RoleImpl.class).toListExplicit(RoleImpl.class);
	}

	public void addRole(Role role) {
		linkIn(role.getImpl(), HAS_ROLE);
	}

	public void removeRole(Role role) {
		unlinkIn(role.getImpl(), HAS_ROLE);
	}

	// TODO add java handler
	public boolean hasRole(Role role) {
		// TODO this is not optimal - research a better way
		return in(HAS_ROLE).has(RoleImpl.class).toListExplicit(RoleImpl.class).contains(role);
	}

	// TODO add java handler
	public boolean hasUser(User user) {
		// TODO this is not optimal - research a better way
		return in(HAS_USER).toList(RoleImpl.class).contains(user);
	}

	/**
	 * Get all users within this group that are visible for the given user.
	 */
	public Page<? extends User> getVisibleUsers(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {

		// @Query(value =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) MATCH (user)-[:MEMBER_OF]-(group:Group) where id(group) = {1} AND requestUser.uuid = {0} and perm.`permissions-read` = true return user ORDER BY user.username",
		// countQuery =
		// "MATCH (requestUser:User)-[:MEMBER_OF]->(group:Group)<-[:HAS_ROLE]-(role:Role)-[perm:HAS_PERMISSION]->(user:User) MATCH (user)-[:MEMBER_OF]-(group:Group) where id(group) = {1} AND requestUser.uuid = {0} and perm.`permissions-read` = true return count(user)")
		// Page<User> findByGroup(String userUuid, Group group, Pageable pageable);
		// return findByGroup(userUuid, group, new MeshPageRequest(pagingInfo));

		// VertexTraversal traversal = requestUser.in(HAS_USER).out(HAS_ROLE).out(Permission.READ_PERM.getLabel()).has(MeshUser.class);
		VertexTraversal<?, ?, ?> traversal = requestUser.getImpl().in(HAS_USER).out(HAS_ROLE).out(Permission.READ_PERM.label()).has(UserImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = requestUser.getImpl().in(HAS_USER).out(HAS_ROLE).out(Permission.READ_PERM.label())
				.has(UserImpl.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, UserImpl.class);
	}

	public Page<? extends Role> getRoles(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {

		VertexTraversal<?, ?, ?> traversal = in(HAS_ROLE);
		VertexTraversal<?, ?, ?> countTraversal = in(HAS_ROLE);

		Page<? extends Role> page = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, RoleImpl.class);
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

	public User createUser(String username) {
		//UserImpl user = getGraph().addFramedVertex(UserImpl.class);
		MeshRoot root = MeshRoot.getInstance();
		User user = root.getUserRoot().create(username);
		addUser(user);
		return user;
	}

	public Role createRole(String name) {
		RoleImpl role = getGraph().addFramedVertex(RoleImpl.class);
		// Add role also to role root
		addRole(role);
		return role;
	}

	@Override
	public void delete() {
		getElement().remove();
	}

	@Override
	public GroupImpl getImpl() {
		return this;
	}
}
