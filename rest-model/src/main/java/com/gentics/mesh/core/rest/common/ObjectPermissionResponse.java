package com.gentics.mesh.core.rest.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.role.RoleReference;

/**
 * Response containing object permissions on all roles
 */
public class ObjectPermissionResponse implements RestModel {
	@JsonProperty(required = true)
	@JsonPropertyDescription("Roles to which the create permission is granted.")
	private List<RoleReference> create;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Roles to which the read permission is granted.")
	private List<RoleReference> read;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Roles to which the update permission is granted.")
	private List<RoleReference> update;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Roles to which the delete permission is granted.")
	private List<RoleReference> delete;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Roles to which the publish permission is granted.")
	private List<RoleReference> publish;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Roles to which the read published permission is granted.")
	private List<RoleReference> readPublished;

	public List<RoleReference> getCreate() {
		return create;
	}

	public ObjectPermissionResponse setCreate(List<RoleReference> create) {
		this.create = create;
		return this;
	}

	public List<RoleReference> getRead() {
		return read;
	}

	public ObjectPermissionResponse setRead(List<RoleReference> read) {
		this.read = read;
		return this;
	}

	public List<RoleReference> getUpdate() {
		return update;
	}

	public ObjectPermissionResponse setUpdate(List<RoleReference> update) {
		this.update = update;
		return this;
	}

	public List<RoleReference> getDelete() {
		return delete;
	}

	public ObjectPermissionResponse setDelete(List<RoleReference> delete) {
		this.delete = delete;
		return this;
	}

	public List<RoleReference> getPublish() {
		return publish;
	}

	public ObjectPermissionResponse setPublish(List<RoleReference> publish) {
		this.publish = publish;
		return this;
	}

	public List<RoleReference> getReadPublished() {
		return readPublished;
	}

	public ObjectPermissionResponse setReadPublished(List<RoleReference> readPublished) {
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
		switch (permission) {
		case CREATE:
			create = add(create, role);
			break;
		case READ:
			read = add(read, role);
			break;
		case UPDATE:
			update = add(update, role);
			break;
		case DELETE:
			delete = add(delete, role);
			break;
		case PUBLISH:
			publish = add(publish, role);
			break;
		case READ_PUBLISHED:
			readPublished = add(readPublished, role);
			break;
		default:
			throw new RuntimeException("Unknown permission type {" + permission.getName() + "}");
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
			create = Collections.emptyList();
		}
		if (read == null) {
			read = Collections.emptyList();
		}
		if (update == null) {
			update = Collections.emptyList();
		}
		if (delete == null) {
			delete = Collections.emptyList();
		}
		if (includePublishPermissions) {
			if (publish == null) {
				publish = Collections.emptyList();
			}
			if (readPublished == null) {
				readPublished = Collections.emptyList();
			}
		}
		return this;
	}

	/**
	 * Get the roles with the given permission
	 * @param perm permission
	 * @return set of role references
	 */
	public List<RoleReference> get(Permission perm) {
		switch (perm) {
		case CREATE:
			return create;
		case READ:
			return read;
		case UPDATE:
			return update;
		case DELETE:
			return delete;
		case PUBLISH:
			return publish;
		case READ_PUBLISHED:
			return readPublished;
		default:
			throw new RuntimeException("Unknown permission type {" + perm.getName() + "}");
		}
	}

	protected List<RoleReference> add(List<RoleReference> set, RoleReference role) {
		if (set == null) {
			set = new ArrayList<>();
		}
		set.add(role);

		return set;
	}
}
