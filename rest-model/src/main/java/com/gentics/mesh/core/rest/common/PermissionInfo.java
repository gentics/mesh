package com.gentics.mesh.core.rest.common;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.PUBLISH;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.READ_PUBLISHED;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.BooleanUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Permission information
 */
public class PermissionInfo implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether the create permission is granted.")
	private Boolean create;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether the read permission is granted.")
	private Boolean read;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether the update permission is granted.")
	private Boolean update;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether the delete permission is granted.")
	private Boolean delete;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether the publish permission is granted.")
	private Boolean publish;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether the read published permission is granted.")
	private Boolean readPublished;

	public static PermissionInfo noPermissions() {
		return new PermissionInfo()
			.setOthers(false);
	}

	public Boolean getRead() {
		return read;
	}

	/**
	 * Set the read permission flag.
	 * 
	 * @param read
	 * @return Fluent API
	 */
	public PermissionInfo setRead(Boolean read) {
		this.read = read;
		return this;
	}

	/**
	 * Return the read permission flag.
	 * 
	 * @return
	 */
	public Boolean getCreate() {
		return create;
	}

	/**
	 * Set the create permission flag.
	 * 
	 * @param create
	 * @return Fluent API
	 */
	public PermissionInfo setCreate(Boolean create) {
		this.create = create;
		return this;
	}

	/**
	 * Return the delete permission flag.
	 * 
	 * @return
	 */
	public Boolean getDelete() {
		return delete;
	}

	/**
	 * Set the delete permission flag.
	 * 
	 * @param delete
	 * @return Fluent API
	 */
	public PermissionInfo setDelete(Boolean delete) {
		this.delete = delete;
		return this;
	}

	/**
	 * Return the publish permission flag.
	 * 
	 * @return
	 */
	public Boolean getPublish() {
		return publish;
	}

	/**
	 * Set the publish permission flag.
	 * 
	 * @param publish
	 * @return Fluent API
	 */
	public PermissionInfo setPublish(Boolean publish) {
		this.publish = publish;
		return this;
	}

	/**
	 * Return the read publish permission flag.
	 * 
	 * @return
	 */
	public Boolean getReadPublished() {
		return readPublished;
	}

	/**
	 * Set the read publish permission flag.
	 * 
	 * @param readPublished
	 * @return Fluent API
	 */
	public PermissionInfo setReadPublished(Boolean readPublished) {
		this.readPublished = readPublished;
		return this;
	}

	public Boolean getUpdate() {
		return update;
	}

	/**
	 * Set the update permission flag.
	 * 
	 * @param update
	 * @return Fluent API
	 */
	public PermissionInfo setUpdate(Boolean update) {
		this.update = update;
		return this;
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
	 * @return Fluent API
	 */
	public PermissionInfo add(Permission permission) {
		set(permission, true);
		return this;
	}

	/**
	 * Set the given permission.
	 * 
	 * @param perm
	 * @param flag
	 * @return Fluent API
	 */
	public PermissionInfo set(Permission perm, boolean flag) {
		switch (perm) {
		case CREATE:
			create = flag;
			break;
		case READ:
			read = flag;
			break;
		case UPDATE:
			update = flag;
			break;
		case DELETE:
			delete = flag;
			break;
		case PUBLISH:
			publish = flag;
			break;
		case READ_PUBLISHED:
			readPublished = flag;
			break;
		default:
			throw new RuntimeException("Unknown permission type {" + perm.getName() + "}");
		}
		return this;
	}

	/**
	 * Set other permissions to the given flag.
	 * 
	 * @param flag
	 * @return Fluent API
	 */
	public PermissionInfo setOthers(boolean flag) {
		return setOthers(flag, true);
	}

	/**
	 * Set other permissions to the given flag.
	 * 
	 * @param flag
	 * @return Fluent API
	 */
	public PermissionInfo setOthers(boolean flag, boolean includePublishPermissions) {
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
		if (includePublishPermissions) {
			if (publish == null) {
				publish = flag;
			}
			if (readPublished == null) {
				readPublished = flag;
			}
		}
		return this;
	}

	/**
	 * Return the boolean value for the given permission.
	 * 
	 * @param perm
	 * @return
	 */
	@JsonIgnore
	public boolean get(Permission perm) {
		return BooleanUtils.isTrue(getNullable(perm));
	}

	/**
	 * Return the boolean value for the given permission.
	 *
	 * @param perm
	 * @return
	 */
	@JsonIgnore
	public Boolean getNullable(Permission perm) {
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

	/**
	 * Return the permissions via a map.
	 * 
	 * @return
	 */
	public Map<Permission, Boolean> asMap() {
		Map<Permission, Boolean> map = new HashMap<>();
		putIfNotNull(map, CREATE, create);
		putIfNotNull(map, READ, read);
		putIfNotNull(map, UPDATE, update);
		putIfNotNull(map, DELETE, delete);
		putIfNotNull(map, PUBLISH, publish);
		putIfNotNull(map, READ_PUBLISHED, readPublished);
		return map;
	}

	private <K, V> void putIfNotNull(Map<K, V> map, K key, V value) {
		if (value != null) {
			map.put(key, value);
		}
	}

	/**
	 * Return the permissions as a set.
	 *
	 * @return
	 */
	public Set<Permission> asSet() {
		Set<Permission> set = new HashSet<>();
		if (create) {
			set.add(CREATE);
		}
		if (read) {
			set.add(READ);
		}
		if (update) {
			set.add(UPDATE);
		}
		if (delete) {
			set.add(DELETE);
		}
		if (publish) {
			set.add(PUBLISH);
		}
		if (readPublished) {
			set.add(READ_PUBLISHED);
		}
		return set;
	}

	/**
	 * Set the permissions from an iterable.
	 * 
	 * @param iterable
	 * @return Fluent API
	 */
	public PermissionInfo fromIterable(Iterable<Permission> iterable) {
		create = false;
		read = false;
		update = false;
		delete = false;
		publish = false;
		readPublished = false;
		for (Permission permission : iterable) {
			set(permission, true);
		}
		return this;
	}

	/**
	 * Set the permissions from an iterable.
	 * 
	 * @param iterable
	 * @return Fluent API
	 */
	public PermissionInfo fromArray(Permission... iterable) {
		create = false;
		read = false;
		update = false;
		delete = false;
		publish = false;
		readPublished = false;
		for (Permission permission : iterable) {
			set(permission, true);
		}
		return this;
	}

	/**
	 * Generate a hash over the permissions.
	 * 
	 * @return Generated hash string
	 */
	@JsonIgnore
	public String getHash() {
		StringBuilder builder = new StringBuilder();
		builder.append(valueOf(create));
		builder.append(valueOf(update));
		builder.append(valueOf(delete));
		builder.append(valueOf(publish));
		builder.append(valueOf(readPublished));
		return builder.toString();
	}

	private int valueOf(Boolean b) {
		if (b == null) {
			return 2;
		} else if (b == true) {
			return 1;
		} else {
			return 0;
		}
	}
}
