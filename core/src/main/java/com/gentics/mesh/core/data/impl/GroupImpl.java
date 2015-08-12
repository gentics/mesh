package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.AbstractGenericVertex;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.TraversalHelper;
import com.syncleus.ferma.traversals.VertexTraversal;

public class GroupImpl extends AbstractGenericVertex<GroupResponse> implements Group {

	public static final String NAME_KEY = "name";

	@Override
	public String getType() {
		return Group.TYPE;
	}

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
		unlinkIn(user.getImpl(), HAS_USER);
		linkIn(user.getImpl(), HAS_USER);
	}

	public void removeUser(User user) {
		unlinkIn(user.getImpl(), HAS_USER);
	}

	public List<? extends Role> getRoles() {
		return in(HAS_ROLE).has(RoleImpl.class).toListExplicit(RoleImpl.class);
	}

	public void addRole(Role role) {
		unlinkIn(role.getImpl(), HAS_ROLE);
		linkIn(role.getImpl(), HAS_ROLE);
	}

	public void removeRole(Role role) {
		unlinkIn(role.getImpl(), HAS_ROLE);
	}

	// TODO add java handler
	public boolean hasRole(Role role) {
		return in(HAS_ROLE).retain(role.getImpl()).hasNext();
	}

	public boolean hasUser(User user) {
		return in(HAS_USER).retain(user.getImpl()).hasNext();
	}

	/**
	 * Get all users within this group that are visible for the given user.
	 */
	public Page<? extends User> getVisibleUsers(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {

		VertexTraversal<?, ?, ?> traversal = in(HAS_USER).mark().in(GraphPermission.READ_PERM.label()).out(HAS_ROLE).in(HAS_USER)
				.retain(requestUser.getImpl()).back().has(UserImpl.class);
		VertexTraversal<?, ?, ?> countTraversal = in(HAS_USER).mark().in(GraphPermission.READ_PERM.label()).out(HAS_ROLE).in(HAS_USER)
				.retain(requestUser.getImpl()).back().has(UserImpl.class);
		return TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, UserImpl.class);
	}

	public Page<? extends Role> getRoles(MeshAuthUser requestUser, PagingInfo pagingInfo) throws InvalidArgumentException {

		VertexTraversal<?, ?, ?> traversal = in(HAS_ROLE);
		VertexTraversal<?, ?, ?> countTraversal = in(HAS_ROLE);

		Page<? extends Role> page = TraversalHelper.getPagedResult(traversal, countTraversal, pagingInfo, RoleImpl.class);
		return page;

	}

	public Group transformToRest(RoutingContext rc, Handler<AsyncResult<GroupResponse>> handler) {
		GroupResponse restGroup = new GroupResponse();
		fillRest(restGroup, rc);
		restGroup.setName(getName());

		// for (User user : group.getUsers()) {
		// String name = user.getUsername();
		// if (name != null) {
		// restGroup.getUsers().add(name);
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

		handler.handle(Future.succeededFuture(restGroup));

		return this;

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
