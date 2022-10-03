package com.gentics.mesh.core.rest.common;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.role.RoleReference;

public class ObjectPermissionResponse implements RestModel {
	@JsonProperty(required = true)
	@JsonPropertyDescription("Roles to which the create permission is granted.")
	private Set<RoleReference> create;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Roles to which the read permission is granted.")
	private Set<RoleReference> read;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Roles to which the update permission is granted.")
	private Set<RoleReference> update;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Roles to which the delete permission is granted.")
	private Set<RoleReference> delete;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Roles to which the publish permission is granted.")
	private Set<RoleReference> publish;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Roles to which the read published permission is granted.")
	private Set<RoleReference> readPublished;

	public Set<RoleReference> getCreate() {
		return create;
	}

	public ObjectPermissionResponse setCreate(Set<RoleReference> create) {
		this.create = create;
		return this;
	}

	public Set<RoleReference> getRead() {
		return read;
	}

	public ObjectPermissionResponse setRead(Set<RoleReference> read) {
		this.read = read;
		return this;
	}

	public Set<RoleReference> getUpdate() {
		return update;
	}

	public ObjectPermissionResponse setUpdate(Set<RoleReference> update) {
		this.update = update;
		return this;
	}

	public Set<RoleReference> getDelete() {
		return delete;
	}

	public ObjectPermissionResponse setDelete(Set<RoleReference> delete) {
		this.delete = delete;
		return this;
	}

	public Set<RoleReference> getPublish() {
		return publish;
	}

	public ObjectPermissionResponse setPublish(Set<RoleReference> publish) {
		this.publish = publish;
		return this;
	}

	public Set<RoleReference> getReadPublished() {
		return readPublished;
	}

	public ObjectPermissionResponse setReadPublished(Set<RoleReference> readPublished) {
		this.readPublished = readPublished;
		return this;
	}

	/**
	 * Set the given permission.
	 * 
	 * @param role role reference
	 * @param permission permission
	 * @return Fluent API
	 */
	public ObjectPermissionResponse add(RoleReference role, Permission permission) {
		set(role, permission, true);
		return this;
	}

	/**
	 * Set the given permission.
	 * 
	 * @param role role reference
	 * @param perm permission
	 * @param flag true to set, false to remove
	 * @return Fluent API
	 */
	public ObjectPermissionResponse set(RoleReference role, Permission perm, boolean flag) {
		switch (perm) {
		case CREATE:
			create = update(create, role, flag);
			break;
		case READ:
			read = update(read, role, flag);
			break;
		case UPDATE:
			update = update(update, role, flag);
			break;
		case DELETE:
			delete = update(delete, role, flag);
			break;
		case PUBLISH:
			publish = update(publish, role, flag);
			break;
		case READ_PUBLISHED:
			readPublished = update(readPublished, role, flag);
			break;
		default:
			throw new RuntimeException("Unknown permission type {" + perm.getName() + "}");
		}
		return this;
	}

	/**
	 * Set other permissions to empty sets
	 * 
	 * @param includePublishPermissions
	 * @return Fluent API
	 */
	public ObjectPermissionResponse setOthers(boolean includePublishPermissions) {
		if (create == null) {
			create = Collections.emptySet();
		}
		if (read == null) {
			read = Collections.emptySet();
		}
		if (update == null) {
			update = Collections.emptySet();
		}
		if (delete == null) {
			delete = Collections.emptySet();
		}
		if (includePublishPermissions) {
			if (publish == null) {
				publish = Collections.emptySet();
			}
			if (readPublished == null) {
				readPublished = Collections.emptySet();
			}
		}
		return this;
	}

	protected Set<RoleReference> update(Set<RoleReference> set, RoleReference role, boolean flag) {
		if (set == null) {
			set = new HashSet<>();
		}

		if (flag) {
			set.add(role);
		} else {
			set.remove(role);
		}

		return set;
	}
}
