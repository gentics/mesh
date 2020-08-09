package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.impl.RoleWrapper;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

import dagger.Lazy;

@Singleton
public class RoleDaoWrapperImpl extends AbstractDaoWrapper implements RoleDaoWrapper {

	private final Lazy<PermissionCache> permissionCache;

	@Inject
	public RoleDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions, Lazy<PermissionCache> permissionCache) {
		super(boot, permissions);
		this.permissionCache = permissionCache;
	}

	@Override
	public RoleResponse transformToRestSync(Role role, InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		RoleResponse restRole = new RoleResponse();

		if (fields.has("name")) {
			restRole.setName(role.getName());
		}

		if (fields.has("groups")) {
			setGroups(role, ac, restRole);
		}
		role.fillCommonRestFields(ac, fields, restRole);

		setRolePermissions(role, ac, restRole);
		return restRole;

	}

	private void setGroups(Role role, InternalActionContext ac, RoleResponse restRole) {
		for (Group group : role.getGroups()) {
			restRole.getGroups().add(group.transformToReference());
		}
	}

	@Override
	public Set<GraphPermission> getPermissions(Role role, MeshVertex vertex) {
		Set<GraphPermission> permissions = new HashSet<>();
		GraphPermission[] possiblePermissions = vertex.hasPublishPermissions()
			? GraphPermission.values()
			: GraphPermission.basicPermissions();

		for (GraphPermission permission : possiblePermissions) {
			if (hasPermission(role, permission, vertex)) {
				permissions.add(permission);
			}
		}
		return permissions;
	}

	@Override
	public boolean hasPermission(Role role, GraphPermission permission, MeshVertex vertex) {
		Set<String> allowedUuids = vertex.property(permission.propertyKey());
		return allowedUuids != null && allowedUuids.contains(role.getUuid());
	}

	@Override
	public void grantPermissions(Role role, MeshVertex vertex, GraphPermission... permissions) {
		for (GraphPermission permission : permissions) {
			Set<String> allowedRoles = vertex.property(permission.propertyKey());
			if (allowedRoles == null) {
				vertex.property(permission.propertyKey(), Collections.singleton(role.getUuid()));
			} else {
				allowedRoles.add(role.getUuid());
				vertex.property(permission.propertyKey(), allowedRoles);
			}
		}
	}

	@Override
	public void revokePermissions(Role role, MeshVertex vertex, GraphPermission... permissions) {
		boolean permissionRevoked = false;
		for (GraphPermission permission : permissions) {
			Set<String> allowedRoles = vertex.property(permission.propertyKey());
			if (allowedRoles != null) {
				permissionRevoked = allowedRoles.remove(role.getUuid()) || permissionRevoked;
				vertex.property(permission.propertyKey(), allowedRoles);
			}
		}

		if (permissionRevoked) {
			permissionCache.get().clear();
		}
	}

	@Override
	public void delete(Role role, BulkActionContext bac) {
		bac.add(role.onDeleted());
		role.getVertex().remove();
		bac.process();
		permissionCache.get().clear();
	}

	@Override
	public boolean update(Role role, InternalActionContext ac, EventQueueBatch batch) {
		RoleUpdateRequest requestModel = ac.fromJson(RoleUpdateRequest.class);
		if (shouldUpdate(requestModel.getName(), role.getName())) {
			// Check for conflict
			Role roleWithSameName = findByName(requestModel.getName());
			if (roleWithSameName != null && !roleWithSameName.getUuid().equals(role.getUuid())) {
				throw conflict(roleWithSameName.getUuid(), requestModel.getName(), "role_conflicting_name");
			}

			role.setName(requestModel.getName());
			batch.add(role.onUpdated());
			return true;
		}
		return false;
	}

	@Override
	public Role findByUuid(String uuid) {
		RoleRoot roleRoot = boot.get().roleRoot();
		return RoleWrapper.wrap(roleRoot.findByUuid(uuid));
	}

	@Override
	public Role findByName(String name) {
		RoleRoot roleRoot = boot.get().roleRoot();
		return RoleWrapper.wrap(roleRoot.findByName(name));
	}

	@Override
	public Page<? extends Group> getGroups(Role role, User user, PagingParameters pagingInfo) {
		RoleRoot roleRoot = boot.get().roleRoot();
		return roleRoot.getGroups(role, user, pagingInfo);
	}

	@Override
	public Role create(String name, User creator, String uuid) {
		RoleRoot roleRoot = boot.get().roleRoot();

		Role role = roleRoot.create();
		if (uuid != null) {
			role.setUuid(uuid);
		}
		role.setName(name);
		role.setCreated(creator);
		addRole(role);
		return role;
	}

	public Role create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		RoleCreateRequest requestModel = ac.fromJson(RoleCreateRequest.class);
		String roleName = requestModel.getName();
		UserDaoWrapper userDao = Tx.get().data().userDao();
		RoleRoot roleRoot = boot.get().roleRoot();

		MeshAuthUser requestUser = ac.getUser();
		if (StringUtils.isEmpty(roleName)) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}

		Role conflictingRole = findByName(roleName);
		if (conflictingRole != null) {
			throw conflict(conflictingRole.getUuid(), roleName, "role_conflicting_name");
		}

		// TODO use non-blocking code here
		if (!userDao.hasPermission(requestUser, roleRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", roleRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}

		Role role = create(requestModel.getName(), requestUser, uuid);
		userDao.inheritRolePermissions(requestUser, roleRoot, role);
		batch.add(role.onCreated());
		return role;

	}

	@Override
	public TraversalResult<? extends Role> findAll() {
		RoleRoot roleRoot = boot.get().roleRoot();
		return roleRoot.findAll();
	}

	@Override
	public void addRole(Role role) {
		RoleRoot roleRoot = boot.get().roleRoot();
		roleRoot.addRole(role);
	}

	@Override
	public void removeRole(Role role) {
		RoleRoot roleRoot = boot.get().roleRoot();
		roleRoot.removeRole(role);
	}

	@Override
	public TransformablePage<? extends Role> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		RoleRoot roleRoot = boot.get().roleRoot();
		return roleRoot.findAll(ac, pagingInfo);
	}

	@Override
	public Role loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm) {
		RoleRoot roleRoot = boot.get().roleRoot();
		return RoleWrapper.wrap(roleRoot.loadObjectByUuid(ac, uuid, perm));
	}

	@Override
	public Role loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound) {
		RoleRoot roleRoot = boot.get().roleRoot();
		return RoleWrapper.wrap(roleRoot.loadObjectByUuid(ac, uuid, perm, errorIfNotFound));
	}
}
