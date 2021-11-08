package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_ROLE_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_ROLE_UNASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_USER_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_USER_UNASSIGNED;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.AbstractCoreDaoWrapper;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.PersistingUserDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.generic.PermissionPropertiesImpl;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.group.GroupRoleAssignModel;
import com.gentics.mesh.core.rest.event.group.GroupUserAssignModel;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

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
	public boolean update(HibGroup group, InternalActionContext ac, EventQueueBatch batch) {
		GroupUpdateRequest requestModel = ac.fromJson(GroupUpdateRequest.class);

		if (isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}

		if (shouldUpdate(requestModel.getName(), group.getName())) {
			HibGroup groupWithSameName = findByName(requestModel.getName());
			if (groupWithSameName != null && !groupWithSameName.getUuid().equals(group.getUuid())) {
				throw conflict(groupWithSameName.getUuid(), requestModel.getName(), "group_conflicting_name", requestModel.getName());
			}

			group.setName(requestModel.getName());

			batch.add(group.onUpdated());
			return true;
		}
		return false;
	}

	@Override
	public HibGroup create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		HibUser requestUser = ac.getUser();
		UserDao userDao = Tx.get().userDao();
		GroupCreateRequest requestModel = ac.fromJson(GroupCreateRequest.class);
		GroupRoot groupRoot = boot.get().meshRoot().getGroupRoot();

		if (StringUtils.isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}
		if (!userDao.hasPermission(requestUser, groupRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", groupRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}

		// Check whether a group with the same name already exists
		HibGroup groupWithSameName = findByName(requestModel.getName());
		// TODO why would we want to check for uuid's here? Makes no sense: && !groupWithSameName.getUuid().equals(getUuid())
		if (groupWithSameName != null) {
			throw conflict(groupWithSameName.getUuid(), requestModel.getName(), "group_conflicting_name", requestModel.getName());
		}

		// Finally create the group and set the permissions
		HibGroup group = create(requestModel.getName(), requestUser, uuid);
		userDao.inheritRolePermissions(requestUser, groupRoot, group);
		batch.add(group.onCreated());
		return group;
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
	public GroupRoleAssignModel createRoleAssignmentEvent(HibGroup group, HibRole role, Assignment assignment) {
		GroupRoleAssignModel model = new GroupRoleAssignModel();
		model.setGroup(group.transformToReference());
		model.setRole(role.transformToReference());
		switch (assignment) {
		case ASSIGNED:
			model.setEvent(GROUP_ROLE_ASSIGNED);
			break;
		case UNASSIGNED:
			model.setEvent(GROUP_ROLE_UNASSIGNED);
			break;
		}
		return model;
	}

	@Override
	public GroupUserAssignModel createUserAssignmentEvent(HibGroup group, HibUser user, Assignment assignment) {
		GroupUserAssignModel model = new GroupUserAssignModel();
		model.setGroup(group.transformToReference());
		model.setUser(user.transformToReference());
		switch (assignment) {
		case ASSIGNED:
			model.setEvent(GROUP_USER_ASSIGNED);
			break;
		case UNASSIGNED:
			model.setEvent(GROUP_USER_UNASSIGNED);
			break;
		}
		return model;
	}

	@Override
	public GroupResponse transformToRestSync(HibGroup group, InternalActionContext ac, int level, String... languageTags) {
		Group graphGroup = toGraph(group);
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		GroupResponse restGroup = new GroupResponse();
		if (fields.has("name")) {
			restGroup.setName(group.getName());
		}
		if (fields.has("roles")) {
			setRoles(group, ac, restGroup);
		}
		graphGroup.fillCommonRestFields(ac, fields, restGroup);

		setRolePermissions(graphGroup, ac, restGroup);
		return restGroup;
	}

	/**
	 * Load the roles that are assigned to this group and add the transformed references to the rest model.
	 *
	 * @param ac
	 * @param restGroup
	 */
	private void setRoles(HibGroup group, InternalActionContext ac, GroupResponse restGroup) {
		for (HibRole role : getRoles(group)) {
			String name = role.getName();
			if (name != null) {
				restGroup.getRoles().add(role.transformToReference());
			}
		}
	}

	@Override
	public void addGroup(HibGroup group) {
		Group graphGroup = toGraph(group);
		GroupRoot groupRoot = boot.get().meshRoot().getGroupRoot();
		groupRoot.addItem(graphGroup);
	}

	@Override
	public void removeGroup(HibGroup group) {
		Group graphGroup = toGraph(group);
		GroupRoot groupRoot = boot.get().meshRoot().getGroupRoot();
		groupRoot.removeItem(graphGroup);
	}

	@Override
	public void delete(HibGroup group, BulkActionContext bac) {
		PersistingUserDao userDao = CommonTx.get().userDao();
		Group graphGroup = toGraph(group);
		// TODO don't allow deletion of the admin group
		bac.batch().add(group.onDeleted());

		Set<? extends HibUser> affectedUsers = getUsers(group).stream().collect(Collectors.toSet());
		graphGroup.getElement().remove();
		for (HibUser user : affectedUsers) {
			userDao.updateShortcutEdges(user);
			bac.add(user.onUpdated());
			bac.inc();
		}
		bac.process();
		permissionCache.get().clear();
	}

	@Override
	public HibGroup create(String name, HibUser creator, String uuid) {
		GroupRoot groupRoot = boot.get().meshRoot().getGroupRoot();
		Group group = groupRoot.create();
		if (uuid != null) {
			group.setUuid(uuid);
		}
		group.setName(name);
		group.setCreated(creator);
		group.generateBucketId();
		addGroup(group);

		return group;
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
