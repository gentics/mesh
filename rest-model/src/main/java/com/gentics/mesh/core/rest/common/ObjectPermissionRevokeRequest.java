package com.gentics.mesh.core.rest.common;

import java.util.List;

import com.gentics.mesh.core.rest.role.RoleReference;

/**
 * Request to revoke permissions from multiple roles
 */
public class ObjectPermissionRevokeRequest extends ObjectPermissionResponse {
	@Override
	public ObjectPermissionRevokeRequest setCreate(List<RoleReference> create) {
		super.setCreate(create);
		return this;
	}

	@Override
	public ObjectPermissionRevokeRequest setRead(List<RoleReference> read) {
		super.setRead(read);
		return this;
	}

	@Override
	public ObjectPermissionRevokeRequest setUpdate(List<RoleReference> update) {
		super.setUpdate(update);
		return this;
	}

	@Override
	public ObjectPermissionRevokeRequest setDelete(List<RoleReference> delete) {
		super.setDelete(delete);
		return this;
	}

	@Override
	public ObjectPermissionRevokeRequest setPublish(List<RoleReference> publish) {
		super.setPublish(publish);
		return this;
	}

	@Override
	public ObjectPermissionRevokeRequest setReadPublished(List<RoleReference> readPublished) {
		super.setReadPublished(readPublished);
		return this;
	}

	@Override
	public ObjectPermissionRevokeRequest add(RoleReference role, Permission permission) {
		super.add(role, permission);
		return this;
	}

	@Override
	public ObjectPermissionRevokeRequest setOthers(boolean includePublishPermissions) {
		super.setOthers(includePublishPermissions);
		return this;
	}
}
