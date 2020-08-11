package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
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
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.impl.GroupWrapper;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.group.GroupRoleAssignModel;
import com.gentics.mesh.core.rest.event.group.GroupUserAssignModel;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

import dagger.Lazy;

@Singleton
public class GroupDaoWrapperImpl extends AbstractDaoWrapper implements GroupDaoWrapper {

	private Lazy<PermissionCache> permissionCache;

	@Inject
	public GroupDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions, Lazy<PermissionCache> permissionCache) {
		super(boot, permissions);
		this.permissionCache = permissionCache;
	}

	@Override
	public boolean update(Group group, InternalActionContext ac, EventQueueBatch batch) {
		GroupUpdateRequest requestModel = ac.fromJson(GroupUpdateRequest.class);

		if (isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}

		if (shouldUpdate(requestModel.getName(), group.getName())) {
			Group groupWithSameName = findByName(requestModel.getName());
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
	public String getETag(Group element, InternalActionContext ac) {
		return boot.get().groupRoot().getETag(element, ac);
	}

	@Override
	public Group create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		MeshAuthUser requestUser = ac.getUser();
		UserDaoWrapper userDao = Tx.get().data().userDao();
		GroupCreateRequest requestModel = ac.fromJson(GroupCreateRequest.class);
		GroupRoot groupRoot = boot.get().groupRoot();

		if (StringUtils.isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}
		if (!userDao.hasPermission(requestUser, groupRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", groupRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}

		// Check whether a group with the same name already exists
		Group groupWithSameName = findByName(requestModel.getName());
		// TODO why would we want to check for uuid's here? Makes no sense: && !groupWithSameName.getUuid().equals(getUuid())
		if (groupWithSameName != null) {
			throw conflict(groupWithSameName.getUuid(), requestModel.getName(), "group_conflicting_name", requestModel.getName());
		}

		// Finally create the group and set the permissions
		Group group = create(requestModel.getName(), requestUser, uuid);
		userDao.inheritRolePermissions(requestUser, groupRoot, group);
		batch.add(group.onCreated());
		return group;
	}

	@Override
	public void addUser(Group group, HibUser user) {
		group.setUniqueLinkInTo(user.toUser(), HAS_USER);

		// Add shortcut edge from user to roles of this group
		for (Role role : getRoles(group)) {
			user.toUser().setUniqueLinkOutTo(role, ASSIGNED_TO_ROLE);
		}
	}

	@Override
	public void removeUser(Group group, HibUser user) {
		group.unlinkIn(user.toUser(), HAS_USER);

		// The user does no longer belong to the group so lets update the shortcut edges
		user.updateShortcutEdges();
		permissionCache.get().clear();
	}

	@Override
	public TraversalResult<? extends HibUser> getUsers(Group group) {
		GroupRoot groupRoot = boot.get().groupRoot();
		return groupRoot.getUsers(group);
	}

	@Override
	public TraversalResult<? extends Role> getRoles(Group group) {
		GroupRoot groupRoot = boot.get().groupRoot();
		return groupRoot.getRoles(group);
	}

	@Override
	public void addRole(Group group, Role role) {
		group.setUniqueLinkInTo(role, HAS_ROLE);

		// Add shortcut edges from role to users of this group
		for (HibUser user : getUsers(group)) {
			((User) user).setUniqueLinkOutTo(role, ASSIGNED_TO_ROLE);
		}

	}

	@Override
	public void removeRole(Group group, Role role) {
		group.unlinkIn(role, HAS_ROLE);

		// Update the shortcut edges since the role does no longer belong to the group
		for (HibUser user : getUsers(group)) {
			user.updateShortcutEdges();
		}
		permissionCache.get().clear();
	}

	@Override
	public boolean hasRole(Group group, Role role) {
		return group.in(HAS_ROLE).retain(role).hasNext();
	}

	@Override
	public boolean hasUser(Group group, HibUser user) {
		return group.in(HAS_USER).retain((User) user).hasNext();
	}

	@Override
	public TransformablePage<? extends HibUser> getVisibleUsers(Group group, MeshAuthUser user, PagingParameters pagingInfo) {
		GroupRoot groupRoot = boot.get().groupRoot();
		return groupRoot.getVisibleUsers(group, user, pagingInfo);
	}

	@Override
	public TransformablePage<? extends Role> getRoles(Group group, HibUser user, PagingParameters pagingInfo) {
		GroupRoot groupRoot = boot.get().groupRoot();
		return groupRoot.getRoles(group, user, pagingInfo);
	}

	@Override
	public GroupRoleAssignModel createRoleAssignmentEvent(Group group, Role role, Assignment assignment) {
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
	public GroupUserAssignModel createUserAssignmentEvent(Group group, HibUser user, Assignment assignment) {
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
	public GroupResponse transformToRestSync(Group group, InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		GroupResponse restGroup = new GroupResponse();
		if (fields.has("name")) {
			restGroup.setName(group.getName());
		}
		if (fields.has("roles")) {
			setRoles(group, ac, restGroup);
		}
		group.fillCommonRestFields(ac, fields, restGroup);

		setRolePermissions(group, ac, restGroup);
		return restGroup;
	}

	/**
	 * Load the roles that are assigned to this group and add the transformed references to the rest model.
	 *
	 * @param ac
	 * @param restGroup
	 */
	private void setRoles(Group group, InternalActionContext ac, GroupResponse restGroup) {
		for (Role role : getRoles(group)) {
			String name = role.getName();
			if (name != null) {
				restGroup.getRoles().add(role.transformToReference());
			}
		}
	}

	@Override
	public void addGroup(Group group) {
		GroupRoot groupRoot = boot.get().groupRoot();
		groupRoot.addItem(group);
	}

	@Override
	public void removeGroup(Group group) {
		GroupRoot groupRoot = boot.get().groupRoot();
		groupRoot.removeItem(group);
	}

	@Override
	public void delete(Group group, BulkActionContext bac) {
		// TODO don't allow deletion of the admin group
		bac.batch().add(group.onDeleted());

		Set<? extends HibUser> affectedUsers = getUsers(group).stream().collect(Collectors.toSet());
		group.getElement().remove();
		for (HibUser user : affectedUsers) {
			user.updateShortcutEdges();
			bac.add(user.onUpdated());
			bac.inc();
		}
		bac.process();
		permissionCache.get().clear();
	}

	@Override
	public Group create(String name, HibUser creator, String uuid) {
		GroupRoot groupRoot = boot.get().groupRoot();
		Group group = groupRoot.create();
		if (uuid != null) {
			group.setUuid(uuid);
		}
		group.setName(name);
		group.setCreated(creator);
		addGroup(group);

		return group;
	}

	@Override
	public Group findByName(String name) {
		GroupRoot groupRoot = boot.get().groupRoot();
		return GroupWrapper.wrap(groupRoot.findByName(name));
	}

	@Override
	public Group findByUuid(String uuid) {
		GroupRoot groupRoot = boot.get().groupRoot();
		return GroupWrapper.wrap(groupRoot.findByUuid(uuid));
	}

	@Override
	public Group findByUuidGlobal(String uuid) {
		return findByUuid(uuid);
	}

	@Override
	public TraversalResult<? extends Group> findAll() {
		GroupRoot groupRoot = boot.get().groupRoot();
		return groupRoot.findAll();
	}

	@Override
	public long computeCount() {
		GroupRoot groupRoot = boot.get().groupRoot();
		return groupRoot.computeCount();
	}

	@Override
	public long computeGlobalCount() {
		return computeCount();
	}

	@Override
	public TransformablePage<? extends Group> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		GroupRoot groupRoot = boot.get().groupRoot();
		return groupRoot.findAll(ac, pagingInfo);
	}
	
	@Override
	public TransformablePage<? extends Group> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<Group> extraFilter) {
		GroupRoot groupRoot = boot.get().groupRoot();
		return groupRoot.findAll(ac, pagingInfo, extraFilter);
	}

	@Override
	public Group loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		GroupRoot groupRoot = boot.get().groupRoot();
		return GroupWrapper.wrap(groupRoot.loadObjectByUuid(ac, uuid, perm));
	}

	@Override
	public Group loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		GroupRoot groupRoot = boot.get().groupRoot();
		return GroupWrapper.wrap(groupRoot.loadObjectByUuid(ac, uuid, perm, errorIfNotFound));
	}

}
