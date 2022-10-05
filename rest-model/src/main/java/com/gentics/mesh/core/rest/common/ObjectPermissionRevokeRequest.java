package com.gentics.mesh.core.rest.common;

import java.util.Set;

import com.gentics.mesh.core.rest.role.RoleReference;

/**
 * Request to revoke permissions from multiple roles
 */
public class ObjectPermissionRevokeRequest extends ObjectPermissionResponse {
	@Override
	public ObjectPermissionRevokeRequest setCreate(Set<RoleReference> create) {
		super.setCreate(create);
		return this;
	}

	@Override
	public ObjectPermissionRevokeRequest setRead(Set<RoleReference> read) {
		super.setRead(read);
		return this;
	}

	@Override
	public ObjectPermissionRevokeRequest setUpdate(Set<RoleReference> update) {
		super.setUpdate(update);
		return this;
	}

	@Override
	public ObjectPermissionRevokeRequest setDelete(Set<RoleReference> delete) {
		super.setDelete(delete);
		return this;
	}

	@Override
	public ObjectPermissionRevokeRequest setPublish(Set<RoleReference> publish) {
		super.setPublish(publish);
		return this;
	}

	@Override
	public ObjectPermissionRevokeRequest setReadPublished(Set<RoleReference> readPublished) {
		super.setReadPublished(readPublished);
		return this;
	}

	@Override
	public ObjectPermissionRevokeRequest add(RoleReference role, Permission permission) {
		super.add(role, permission);
		return this;
	}

	@Override
	public ObjectPermissionRevokeRequest set(RoleReference role, Permission perm, boolean flag) {
		super.set(role, perm, flag);
		return this;
	}

	@Override
	public ObjectPermissionRevokeRequest setOthers(boolean includePublishPermissions) {
		super.setOthers(includePublishPermissions);
		return this;
	}
}
