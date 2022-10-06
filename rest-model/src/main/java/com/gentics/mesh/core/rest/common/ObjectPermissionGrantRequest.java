package com.gentics.mesh.core.rest.common;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.role.RoleReference;

/**
 * Request to grant permissions to multiple roles
 */
public class ObjectPermissionGrantRequest extends ObjectPermissionResponse {
	@JsonProperty(required = false, defaultValue = "false")
	@JsonPropertyDescription("Flag which indicates whether the permissions granted to only the given roles (will be revoked from all other roles).")
	private boolean exclusive = false;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Roles which are ignored when the exclusive flag is set.")
	private List<RoleReference> ignore;

	/**
	 * Flag that indicated that the request should be executed exclusively.
	 *
	 * @return Flag value
	 */
	public boolean isExclusive() {
		return exclusive;
	}

	/**
	 * Set the flag which indicated whether the permission changes should be applied exclusively.
	 *
	 * @param exclusive
	 *            Flag value
	 * @return Fluent API
	 */
	public ObjectPermissionGrantRequest setExclusive(boolean exclusive) {
		this.exclusive = exclusive;
		return this;
	}

	public List<RoleReference> getIgnore() {
		return ignore;
	}

	public ObjectPermissionGrantRequest setIgnore(List<RoleReference> ignore) {
		this.ignore = ignore;
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest setCreate(List<RoleReference> create) {
		super.setCreate(create);
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest setRead(List<RoleReference> read) {
		super.setRead(read);
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest setUpdate(List<RoleReference> update) {
		super.setUpdate(update);
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest setDelete(List<RoleReference> delete) {
		super.setDelete(delete);
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest setPublish(List<RoleReference> publish) {
		super.setPublish(publish);
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest setReadPublished(List<RoleReference> readPublished) {
		super.setReadPublished(readPublished);
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest add(RoleReference role, Permission permission) {
		super.add(role, permission);
		return this;
	}

	@Override
	public ObjectPermissionGrantRequest setOthers(boolean includePublishPermissions) {
		super.setOthers(includePublishPermissions);
		return this;
	}
}
