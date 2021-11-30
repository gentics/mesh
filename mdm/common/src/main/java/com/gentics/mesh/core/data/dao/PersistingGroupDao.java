package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
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
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.group.GroupRoleAssignModel;
import com.gentics.mesh.core.rest.event.group.GroupUserAssignModel;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

/**
 * A persisting extension to {@link SchemaDao}
 * 
 * @author plyhun
 *
 */
public interface PersistingGroupDao extends GroupDao, PersistingDaoGlobal<HibGroup> {

	@Override
	default HibGroup create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		HibUser requestUser = ac.getUser();
		UserDao userDao = Tx.get().userDao();
		GroupCreateRequest requestModel = ac.fromJson(GroupCreateRequest.class);

		if (StringUtils.isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}
		HibBaseElement groupPermissionRoot = Tx.get().data().permissionRoots().group();
		if (!userDao.hasPermission(requestUser, groupPermissionRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", groupPermissionRoot.getUuid(),
					CREATE_PERM.getRestPerm().getName());
		}

		// Check whether a group with the same name already exists
		HibGroup groupWithSameName = findByName(requestModel.getName());
		// TODO why would we want to check for uuid's here? Makes no sense: &&
		// !groupWithSameName.getUuid().equals(getUuid())
		if (groupWithSameName != null) {
			throw conflict(groupWithSameName.getUuid(), requestModel.getName(), "group_conflicting_name",
					requestModel.getName());
		}

		// Finally create the group and set the permissions
		HibGroup group = create(requestModel.getName(), requestUser, uuid);
		userDao.inheritRolePermissions(requestUser, groupPermissionRoot, group);
		batch.add(group.onCreated());
		return group;
	}

	@Override
	default HibGroup create(String name, HibUser user, String uuid) {
		HibGroup group = createPersisted(uuid);
		group.setName(name);
		group.setCreated(user);
		group.generateBucketId();
		return group;
	}

	@Override
	default GroupRoleAssignModel createRoleAssignmentEvent(HibGroup group, HibRole role, Assignment assignment) {
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
	default GroupUserAssignModel createUserAssignmentEvent(HibGroup group, HibUser user, Assignment assignment) {
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
	default boolean update(HibGroup group, InternalActionContext ac, EventQueueBatch batch) {
		GroupUpdateRequest requestModel = ac.fromJson(GroupUpdateRequest.class);

		if (isEmpty(requestModel.getName())) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}

		if (shouldUpdate(requestModel.getName(), group.getName())) {
			HibGroup groupWithSameName = findByName(requestModel.getName());
			if (groupWithSameName != null && !groupWithSameName.getUuid().equals(group.getUuid())) {
				throw conflict(groupWithSameName.getUuid(), requestModel.getName(), "group_conflicting_name",
						requestModel.getName());
			}

			group.setName(requestModel.getName());

			batch.add(group.onUpdated());
			return true;
		}

		return false;
	}

	@Override
	default void delete(HibGroup group, BulkActionContext bac) {
		PersistingUserDao userDao = CommonTx.get().userDao();

		// TODO unhardcode the admin name
		if ("admin".equals(group.getName())) {
			throw error(FORBIDDEN, "error_illegal_admin_deletion");
		}
		bac.batch().add(group.onDeleted());

		Set<? extends HibUser> affectedUsers = getUsers(group).stream().collect(Collectors.toSet());

		deletePersisted(group);

		for (HibUser affectedUser : affectedUsers) {
			userDao.updateShortcutEdges(affectedUser);
			bac.add(affectedUser.onUpdated());
			bac.inc();
		}
		bac.process();

		Tx.get().permissionCache().clear();
	}

	@Override
	default GroupResponse transformToRestSync(HibGroup group, InternalActionContext ac, int level,
			String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		GroupResponse restGroup = new GroupResponse();
		if (fields.has("name")) {
			restGroup.setName(group.getName());
		}
		if (fields.has("roles")) {
			for (HibRole role : getRoles(group)) {
				String name = role.getName();
				if (name != null) {
					restGroup.getRoles().add(role.transformToReference());
				}
			}
		}
		group.fillCommonRestFields(ac, fields, restGroup);
		Tx.get().roleDao().setRolePermissions(group, ac, restGroup);
		return restGroup;
	}

	@Override
	default void removeUser(HibGroup group, HibUser user) {
		removeUser(user, group);
		Tx.get().permissionCache().clear();
	}

	void removeUser(HibUser user, HibGroup group);

	@Override
	default void removeRole(HibGroup group, HibRole role) {
		removeRole(role, group);
		Tx.get().permissionCache().clear();
	}

	void removeRole(HibRole role, HibGroup group);
}
