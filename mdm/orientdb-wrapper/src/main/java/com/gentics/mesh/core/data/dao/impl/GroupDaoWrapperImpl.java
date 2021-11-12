package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.AbstractCoreDaoWrapper;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.PersistingUserDao;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

/**
 * DAO for {@link HibGroup}
 * 
 * TODO MDM The methods should be moved to {@link HibGroup}
 */
@Singleton
public class GroupDaoWrapperImpl extends AbstractCoreDaoWrapper<GroupResponse, HibGroup, Group> implements GroupDaoWrapper {

	private Lazy<PermissionCache> permissionCache;

	@Inject
	public GroupDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot, Lazy<PermissionPropertiesImpl> permissions, Lazy<PermissionCache> permissionCache) {
		super(boot, permissions);
		this.permissionCache = permissionCache;
	}

	@Override
	public void addUser(HibGroup group, HibUser user) {
		Group graphGroup = toGraph(group);
		User graphUser = toGraph(user);

		graphGroup.setUniqueLinkInTo(graphUser, HAS_USER);

		// Add shortcut edge from user to roles of this group
		for (HibRole role : getRoles(group)) {
			graphUser.setUniqueLinkOutTo(toGraph(role), ASSIGNED_TO_ROLE);
		}
	}

	@Override
	public void removeUser(HibGroup group, HibUser user) {
		PersistingUserDao userDao = CommonTx.get().userDao();
		Group graphGroup = toGraph(group);
		User graphUser = toGraph(user);

		graphGroup.unlinkIn(graphUser, HAS_USER);

		// The user does no longer belong to the group so lets update the shortcut edges
		userDao.updateShortcutEdges(user);
		permissionCache.get().clear();
	}

	@Override
	public Result<? extends HibUser> getUsers(HibGroup group) {
		Group graphGroup = toGraph(group);
		GroupRoot groupRoot = boot.get().meshRoot().getGroupRoot();
		return groupRoot.getUsers(graphGroup);
	}

	@Override
	public Result<? extends HibRole> getRoles(HibGroup group) {
		Group graphGroup = toGraph(group);
		GroupRoot groupRoot = boot.get().meshRoot().getGroupRoot();
		return groupRoot.getRoles(graphGroup);
	}

	@Override
	public void addRole(HibGroup group, HibRole role) {
		Group graphGroup = toGraph(group);
		Role graphRole = toGraph(role);
		graphGroup.setUniqueLinkInTo(graphRole, HAS_ROLE);

		// Add shortcut edges from role to users of this group
		for (HibUser user : getUsers(group)) {
			toGraph(user).setUniqueLinkOutTo(graphRole, ASSIGNED_TO_ROLE);
		}

	}

	@Override
	public void removeRole(HibGroup group, HibRole role) {
		PersistingUserDao userDao = CommonTx.get().userDao();
		Role graphRole = toGraph(role);
		Group graphGroup = toGraph(group);
		graphGroup.unlinkIn(graphRole, HAS_ROLE);

		// Update the shortcut edges since the role does no longer belong to the group
		for (HibUser user : getUsers(group)) {
			userDao.updateShortcutEdges(user);
		}
		permissionCache.get().clear();
	}

	@Override
	public boolean hasRole(HibGroup group, HibRole role) {
		Role graphRole = toGraph(role);
		Group graphGroup = toGraph(group);
		return graphGroup.in(HAS_ROLE).retain(graphRole).hasNext();
	}

	@Override
	public boolean hasUser(HibGroup group, HibUser user) {
		Group graphGroup = toGraph(group);
		return graphGroup.in(HAS_USER).retain(toGraph(user)).hasNext();
	}

	@Override
	public Page<? extends HibUser> getVisibleUsers(HibGroup group, HibUser user, PagingParameters pagingInfo) {
		GroupRoot groupRoot = boot.get().meshRoot().getGroupRoot();
		Group graphGroup = toGraph(group);
		return groupRoot.getVisibleUsers(graphGroup, user, pagingInfo);
	}

	@Override
	public Page<? extends Role> getRoles(HibGroup group, HibUser user, PagingParameters pagingInfo) {
		GroupRoot groupRoot = boot.get().meshRoot().getGroupRoot();
		Group graphGroup = toGraph(group);
		return groupRoot.getRoles(graphGroup, user, pagingInfo);
	}

	@Override
	public HibGroup findByName(String name) {
		GroupRoot groupRoot = boot.get().meshRoot().getGroupRoot();
		return groupRoot.findByName(name);
	}

	@Override
	public HibGroup findByUuid(String uuid) {
		GroupRoot groupRoot = boot.get().meshRoot().getGroupRoot();
		return groupRoot.findByUuid(uuid);
	}

	@Override
	public Result<? extends HibGroup> findAll() {
		GroupRoot groupRoot = boot.get().meshRoot().getGroupRoot();
		return groupRoot.findAll();
	}

	@Override
	public long count() {
		GroupRoot groupRoot = boot.get().meshRoot().getGroupRoot();
		return groupRoot.globalCount();
	}

	@Override
	public Page<? extends HibGroup> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		GroupRoot groupRoot = boot.get().meshRoot().getGroupRoot();
		return groupRoot.findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibGroup> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibGroup> extraFilter) {
		GroupRoot groupRoot = boot.get().meshRoot().getGroupRoot();
		return groupRoot.findAll(ac, pagingInfo, group -> {
			return extraFilter.test(group);
		});
	}

	@Override
	public String getAPIPath(HibGroup group, InternalActionContext ac) {
		Group graphGroup = toGraph(group);
		return graphGroup.getAPIPath(ac);
	}

	@Override
	public String getETag(HibGroup group, InternalActionContext ac) {
		Group graphGroup = toGraph(group);
		return graphGroup.getETag(ac);
		// return boot.get().meshRoot().getGroupRoot().getETag(graphGroup, ac);
	}

	@Override
	protected RootVertex<Group> getRoot() {
		return boot.get().meshRoot().getGroupRoot();
	}

}
