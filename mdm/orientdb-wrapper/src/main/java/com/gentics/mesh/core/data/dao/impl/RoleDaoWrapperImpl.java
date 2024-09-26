package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.util.HibClassConverter.toGraph;

import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.OrientDBBootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.dao.AbstractCoreDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.parameter.PagingParameters;

import dagger.Lazy;

/**
 * DAO for {@link HibRole} operations.
 */
@Singleton
public class RoleDaoWrapperImpl extends AbstractCoreDaoWrapper<RoleResponse, HibRole, Role> implements RoleDaoWrapper {

	@Inject
	public RoleDaoWrapperImpl(Lazy<OrientDBBootstrapInitializer> boot) {
		super(boot);
	}

	@Override
	public boolean grantRolePermissions(HibRole role, HibBaseElement element, InternalPermission... permissions) {
		MeshVertex vertex = (MeshVertex) element;
		boolean permissionGranted = false;
		for (InternalPermission permission : permissions) {
			Set<String> allowedRoles = getRoleUuidsForPerm(vertex, permission);
			if (allowedRoles == null) {
				vertex.setRoleUuidForPerm(permission, Collections.singleton(role.getUuid()));
				permissionGranted = true;
			} else {
				permissionGranted = allowedRoles.add(role.getUuid()) || permissionGranted;
				vertex.setRoleUuidForPerm(permission, allowedRoles);
			}
		}
		return permissionGranted;
	}

	@Override
	public boolean grantRolePermissions(Set<HibRole> roles, HibBaseElement element, boolean exclusive,
			InternalPermission... permissions) {
		Set<String> roleUuids = roles.stream().map(HibRole::getUuid).collect(Collectors.toSet());
		return grantRolePermissionsWithUuids(roleUuids, element, exclusive, permissions);
	}

	@Override
	public boolean grantRolePermissionsWithUuids(Set<String> roleUuids, HibBaseElement element, boolean exclusive,
			InternalPermission... permissions) {
		MeshVertex vertex = (MeshVertex) element;
		boolean permissionGranted = false;
		for (InternalPermission permission : permissions) {
			Set<String> allowedRoles = getRoleUuidsForPerm(vertex, permission);

			if (allowedRoles == null) {
				vertex.setRoleUuidForPerm(permission, roleUuids);
				if (!roleUuids.isEmpty()) {
					permissionGranted = true;
				}
			} else {
				permissionGranted = allowedRoles.addAll(roleUuids) || permissionGranted;
				if (exclusive) {
					permissionGranted = allowedRoles.retainAll(roleUuids) || permissionGranted;
				}

				vertex.setRoleUuidForPerm(permission, allowedRoles);
			}
		}

		return permissionGranted;
	}

	@Override
	public boolean revokeRolePermissions(HibRole role, HibBaseElement element, InternalPermission... permissions) {
		MeshVertex vertex = (MeshVertex) element;
		boolean permissionRevoked = false;
		for (InternalPermission permission : permissions) {
			Set<String> allowedRoles = getRoleUuidsForPerm(vertex, permission);
			if (allowedRoles != null) {
				permissionRevoked = allowedRoles.remove(role.getUuid()) || permissionRevoked;
				vertex.setRoleUuidForPerm(permission, allowedRoles);
			}
		}

		return permissionRevoked;
	}

	@Override
	public boolean revokeRolePermissions(Set<HibRole> roles, HibBaseElement element, InternalPermission... permissions) {
		Set<String> roleUuids = roles.stream().map(HibRole::getUuid).collect(Collectors.toSet());
		return revokeRolePermissionsWithUuids(roleUuids, element, permissions);
	}

	@Override
	public boolean revokeRolePermissionsWithUuids(Set<String> roleUuids, HibBaseElement element,
			InternalPermission... permissions) {
		MeshVertex vertex = (MeshVertex) element;

		boolean permissionRevoked = false;
		for (InternalPermission permission : permissions) {
			Set<String> allowedRoles = getRoleUuidsForPerm(vertex, permission);
			if (allowedRoles != null) {
				for (String roleUuid : roleUuids) {
					permissionRevoked = allowedRoles.remove(roleUuid) || permissionRevoked;
				}
				vertex.setRoleUuidForPerm(permission, allowedRoles);
			}
		}

		return permissionRevoked;
	}

	@Override
	public HibRole findByUuid(String uuid) {
		RoleRoot roleRoot = boot.get().meshRoot().getRoleRoot();
		return roleRoot.findByUuid(uuid);
	}

	@Override
	public HibRole findByName(String name) {
		RoleRoot roleRoot = boot.get().meshRoot().getRoleRoot();
		return roleRoot.findByName(name);
	}

	@Override
	public Page<? extends HibGroup> getGroups(HibRole role, HibUser user, PagingParameters pagingInfo) {
		Role graphRole = toGraph(role);
		RoleRoot roleRoot = boot.get().meshRoot().getRoleRoot();
		return roleRoot.getGroups(graphRole, user, pagingInfo);
	}

	@Override
	public Result<? extends HibGroup> getGroups(HibRole role) {
		Role graphRole = toGraph(role);
		return graphRole.getGroups();
	}

	@Override
	public void addRole(HibRole role) {
		Role graphRole = toGraph(role);
		RoleRoot roleRoot = boot.get().meshRoot().getRoleRoot();
		roleRoot.addRole(graphRole);
	}

	@Override
	public void removeRole(HibRole role) {
		Role graphRole = toGraph(role);
		RoleRoot roleRoot = boot.get().meshRoot().getRoleRoot();
		roleRoot.removeRole(graphRole);
	}

	@Override
	public Page<? extends HibRole> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibRole> extraFilter) {
		RoleRoot roleRoot = boot.get().meshRoot().getRoleRoot();
		return roleRoot.findAll(ac, pagingInfo, role -> {
			return extraFilter.test(role);
		});
	}

	@Override
	public long count() {
		RoleRoot roleRoot = boot.get().meshRoot().getRoleRoot();
		return roleRoot.globalCount();
	}

	@Override
	public Set<String> getRoleUuidsForPerm(HibBaseElement element, InternalPermission permission) {
		return (element instanceof MeshVertex) 
				? ((MeshVertex) element).getRoleUuidsForPerm(permission)
				: Collections.emptySet();
	}

	@Override
	public Result<? extends HibRole> findAll() {
		RoleRoot roleRoot = boot.get().meshRoot().getRoleRoot();
		return roleRoot.findAll();
	}

	@Override
	protected RootVertex<Role> getRoot() {
		return boot.get().meshRoot().getRoleRoot();
	}
}
