package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cache.PermissionCache;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.dao.AbstractDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.data.dao.util.CommonDaoHelper;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

import dagger.Lazy;

@Singleton
public class RoleDaoWrapperImpl extends AbstractDaoWrapper<HibRole> implements RoleDaoWrapper {

	private final Lazy<PermissionCache> permissionCache;
	private final CommonDaoHelper commonDaoHelper;

	@Inject
	public RoleDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions, Lazy<PermissionCache> permissionCache, CommonDaoHelper commonDaoHelper) {
		super(boot, permissions);
		this.permissionCache = permissionCache;
		this.commonDaoHelper = commonDaoHelper;
	}

	@Override
	public RoleResponse transformToRestSync(HibRole role, InternalActionContext ac, int level, String... languageTags) {
		Role graphRole = toGraph(role);
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		RoleResponse restRole = new RoleResponse();

		if (fields.has("name")) {
			restRole.setName(role.getName());
		}

		if (fields.has("groups")) {
			setGroups(role, ac, restRole);
		}
		graphRole.fillCommonRestFields(ac, fields, restRole);

		setRolePermissions(graphRole, ac, restRole);
		return restRole;

	}

	private void setGroups(HibRole role, InternalActionContext ac, RoleResponse restRole) {
		Role graphRole = toGraph(role);
		for (Group group : graphRole.getGroups()) {
			restRole.getGroups().add(group.transformToReference());
		}
	}

	@Override
	public Set<InternalPermission> getPermissions(HibRole role, HibBaseElement element) {
		Set<InternalPermission> permissions = new HashSet<>();
		InternalPermission[] possiblePermissions = element.hasPublishPermissions()
			? InternalPermission.values()
			: InternalPermission.basicPermissions();

		for (InternalPermission permission : possiblePermissions) {
			if (hasPermission(role, permission, element)) {
				permissions.add(permission);
			}
		}
		return permissions;
	}

	@Override
	public boolean hasPermission(HibRole role, InternalPermission permission, HibBaseElement vertex) {
		Set<String> allowedUuids = getRoleUuidsForPerm(vertex, permission);
		return allowedUuids != null && allowedUuids.contains(role.getUuid());
	}

	@Override
	public void grantPermissions(HibRole role, HibBaseElement vertex, InternalPermission... permissions) {
		for (InternalPermission permission : permissions) {
			Set<String> allowedRoles = getRoleUuidsForPerm(vertex, permission);
			if (allowedRoles == null) {
				vertex.setRoleUuidForPerm(permission, Collections.singleton(role.getUuid()));
			} else {
				allowedRoles.add(role.getUuid());
				vertex.setRoleUuidForPerm(permission, allowedRoles);
			}
		}
	}

	@Override
	public void revokePermissions(HibRole role, HibBaseElement vertex, InternalPermission... permissions) {
		boolean permissionRevoked = false;
		for (InternalPermission permission : permissions) {
			Set<String> allowedRoles = getRoleUuidsForPerm(vertex, permission);
			if (allowedRoles != null) {
				permissionRevoked = allowedRoles.remove(role.getUuid()) || permissionRevoked;
				vertex.setRoleUuidForPerm(permission, allowedRoles);
			}
		}

		if (permissionRevoked) {
			permissionCache.get().clear();
		}
	}

	@Override
	public void delete(HibRole role, BulkActionContext bac) {
		bac.add(role.onDeleted());
		role.removeElement();
		bac.process();
		permissionCache.get().clear();
	}

	@Override
	public boolean update(HibRole role, InternalActionContext ac, EventQueueBatch batch) {
		RoleUpdateRequest requestModel = ac.fromJson(RoleUpdateRequest.class);
		if (shouldUpdate(requestModel.getName(), role.getName())) {
			// Check for conflict
			HibRole roleWithSameName = findByName(requestModel.getName());
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
	public HibRole findByUuid(String uuid) {
		RoleRoot roleRoot = boot.get().roleRoot();
		return roleRoot.findByUuid(uuid);
	}

	@Override
	public HibRole findByUuidGlobal(String uuid) {
		return findByUuid(uuid);
	}

	@Override
	public HibRole findByName(String name) {
		RoleRoot roleRoot = boot.get().roleRoot();
		return roleRoot.findByName(name);
	}

	@Override
	public Page<? extends HibGroup> getGroups(HibRole role, HibUser user, PagingParameters pagingInfo) {
		Role graphRole = toGraph(role);
		RoleRoot roleRoot = boot.get().roleRoot();
		return roleRoot.getGroups(graphRole, user, pagingInfo);
	}

	@Override
	public Result<? extends HibGroup> getGroups(HibRole role) {
		Role graphRole = toGraph(role);
		return graphRole.getGroups();
	}

	@Override
	public HibRole create(String name, HibUser creator, String uuid) {
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

	public HibRole create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		RoleCreateRequest requestModel = ac.fromJson(RoleCreateRequest.class);
		String roleName = requestModel.getName();
		UserDaoWrapper userDao = Tx.get().userDao();
		RoleRoot roleRoot = boot.get().roleRoot();

		MeshAuthUser requestUser = ac.getUser();
		if (StringUtils.isEmpty(roleName)) {
			throw error(BAD_REQUEST, "error_name_must_be_set");
		}

		HibRole conflictingRole = findByName(roleName);
		if (conflictingRole != null) {
			throw conflict(conflictingRole.getUuid(), roleName, "role_conflicting_name");
		}

		// TODO use non-blocking code here
		if (!userDao.hasPermission(requestUser, roleRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", roleRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}

		HibRole role = create(requestModel.getName(), requestUser, uuid);
		userDao.inheritRolePermissions(requestUser, roleRoot, role);
		batch.add(role.onCreated());
		return role;

	}

	@Override
	public Result<? extends HibRole> findAll() {
		RoleRoot roleRoot = boot.get().roleRoot();
		return roleRoot.findAll();
	}

	@Override
	public void addRole(HibRole role) {
		Role graphRole = toGraph(role);
		RoleRoot roleRoot = boot.get().roleRoot();
		roleRoot.addRole(graphRole);
	}

	@Override
	public void removeRole(HibRole role) {
		Role graphRole = toGraph(role);
		RoleRoot roleRoot = boot.get().roleRoot();
		roleRoot.removeRole(graphRole);
	}

	@Override
	public TransformablePage<? extends HibRole> findAll(InternalActionContext ac, PagingParameters pagingInfo) {
		RoleRoot roleRoot = boot.get().roleRoot();
		return roleRoot.findAll(ac, pagingInfo);
	}

	@Override
	public Page<? extends HibRole> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibRole> extraFilter) {
		RoleRoot roleRoot = boot.get().roleRoot();
		return roleRoot.findAll(ac, pagingInfo, role -> {
			return extraFilter.test(role);
		});
	}

	@Override
	public HibRole loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm) {
		RoleRoot roleRoot = boot.get().roleRoot();
		return roleRoot.loadObjectByUuid(ac, uuid, perm);
	}

	@Override
	public HibRole loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound) {
		RoleRoot roleRoot = boot.get().roleRoot();
		return roleRoot.loadObjectByUuid(ac, uuid, perm, errorIfNotFound);
	}

	@Override
	public long computeGlobalCount() {
		RoleRoot roleRoot = boot.get().roleRoot();
		return roleRoot.computeCount();
	}

	@Override
	public String getAPIPath(HibRole role, InternalActionContext ac) {
		return commonDaoHelper.getRootLevelAPIPath(ac, role);
	}

	@Override
	public String getETag(HibRole role, InternalActionContext ac) {
		Role graphRole = toGraph(role);
		return graphRole.getETag(ac);
	}

	@Override
	public void applyPermissions(HibBaseElement element, EventQueueBatch batch, HibRole role, boolean recursive, Set<InternalPermission> permissionsToGrant,
		Set<InternalPermission> permissionsToRevoke) {
		Role graphRole = toGraph(role);
		MeshVertex graphElement = (MeshVertex) element;
		graphElement.applyPermissions(batch, graphRole, recursive, permissionsToGrant, permissionsToRevoke);
	}

	@Override
	public Set<String> getRoleUuidsForPerm(HibBaseElement element, InternalPermission permission) {
		return ((MeshVertex) element).getRoleUuidsForPerm(permission);
	}
}
