package com.gentics.mesh.core.rest.common;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.PUBLISH;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.READ_PUBLISHED;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Permission information
 */
public class PermissionInfo implements RestModel {

	private Boolean create;

	private Boolean read;

	private Boolean update;

	private Boolean delete;

	private Boolean publish;

	private Boolean readPublished;

	public Boolean getRead() {
		return read;
	}

	public void setRead(Boolean read) {
		this.read = read;
	}

	public Boolean getCreate() {
		return create;
	}

	public void setCreate(Boolean create) {
		this.create = create;
	}

	public Boolean getDelete() {
		return delete;
	}

	public void setDelete(Boolean delete) {
		this.delete = delete;
	}

	public Boolean getPublish() {
		return publish;
	}

	public void setPublish(Boolean publish) {
		this.publish = publish;
	}

	public Boolean getReadPublished() {
		return readPublished;
	}

	public void setReadPublished(Boolean readPublished) {
		this.readPublished = readPublished;
	}

	public Boolean getUpdate() {
		return update;
	}

	public void setUpdate(Boolean update) {
		this.update = update;
	}

	/**
	 * Check whether the given permission is listed as granted.
	 * 
	 * @param permission
	 * @return
	 */
	public boolean hasPerm(Permission permission) {
		return get(permission);
	}

	/**
	 * Set the given permission.
	 * 
	 * @param permission
	 */
	public void add(Permission permission) {
		set(permission, true);
	}

	/**
	 * Set the given permission.
	 * 
	 * @param perm
	 * @param flag
	 */
	public void set(Permission perm, boolean flag) {
		switch (perm) {
		case CREATE:
			create = flag;
		case READ:
			read = flag;
			return;
		case UPDATE:
			update = flag;
			return;
		case DELETE:
			delete = flag;
			return;
		case PUBLISH:
			publish = flag;
			return;
		case READ_PUBLISHED:
			readPublished = flag;
			return;
		default:
			throw new RuntimeException("Unknown permission type {" + perm.getName() + "}");
		}
	}

	/**
	 * Set other permissions to the given flag.
	 * 
	 * @param flag
	 */
	public void setOthers(boolean flag) {
		if (create == null) {
			create = flag;
		}
		if (read == null) {
			read = flag;
		}
		if (update == null) {
			update = flag;
		}
		if (delete == null) {
			delete = flag;
		}
		if (publish == null) {
			publish = flag;
		}
		if (readPublished == null) {
			readPublished = flag;
		}

	}

	/**
	 * Return the boolean value for the given permission.
	 * 
	 * @param perm
	 * @return
	 */
	@JsonIgnore
	public boolean get(Permission perm) {
		switch (perm) {
		case CREATE:
			return create == null ? false : create;
		case READ:
			return read == null ? false : read;
		case UPDATE:
			return update == null ? false : update;
		case DELETE:
			return delete == null ? false : delete;
		case PUBLISH:
			return publish == null ? false : publish;
		case READ_PUBLISHED:
			return readPublished == null ? false : readPublished;
		default:
			throw new RuntimeException("Unknown permission type {" + perm.getName() + "}");
		}
	}

	/**
	 * Return the permissions via a map.
	 * 
	 * @return
	 */
	public Map<Permission, Boolean> asMap() {
		Map<Permission, Boolean> map = new HashMap<>();
		map.put(CREATE, create);
		map.put(READ, read);
		map.put(UPDATE, update);
		map.put(DELETE, delete);
		map.put(PUBLISH, publish);
		map.put(READ_PUBLISHED, readPublished);
		return map;
	}

}
