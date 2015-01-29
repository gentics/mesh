package com.gentics.vertx.cailun.perm.model;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.annotation.Transient;
import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

import com.gentics.vertx.cailun.base.model.AbstractPersistable;
import com.gentics.vertx.cailun.base.model.GenericNode;

/**
 * The permission object is an element that is used to form the ACL domain in the graph.
 * 
 * @author johannes2
 *
 */
@RelationshipEntity
public class PermissionSet extends AbstractPersistable {

	private static final long serialVersionUID = 1564260534291371364L;

	public static final String READ = "read";
	public static final String DELETE = "delete";
	public static final String CREATE = "create";
	public static final String MODIFY = "modify";

	public static final String RELATION_KEYWORD = "HAS_PERMISSIONSET";

	@Fetch
	@StartNode
	private Role role;

	@Fetch
	@EndNode
	private GenericNode object;

	@Fetch
	private boolean canRead = false;

	@Transient
	private GenericPermission readPermission = null;

	@Fetch
	private boolean canModify = false;

	@Transient
	private GenericPermission modifyPermission = null;

	@Fetch
	private boolean canDelete = false;

	@Transient
	private GenericPermission deletePermission = null;

	@Fetch
	private boolean canCreate = false;

	@Transient
	private GenericPermission createPermission = null;

	public PermissionSet() {
	}

	@PersistenceConstructor
	public PermissionSet(Role role, GenericNode object) {
		this.role = role;
		this.object = object;
	}

	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public GenericNode getObject() {
		return object;
	}

	public void setObject(GenericNode object) {
		this.object = object;
	}

	public GenericPermission getCreatePermission() {
		// lazy loading/update of transient field
		if (canCreate && createPermission == null) {
			createPermission = new GenericPermission(object, CREATE);
		}
		return createPermission;
	}

	public GenericPermission getDeletePermission() {
		// lazy loading/update of transient field
		if (canDelete && deletePermission == null) {
			deletePermission = new GenericPermission(object, DELETE);
		}
		return deletePermission;
	}

	public GenericPermission getModifyPermission() {
		// lazy loading/update of transient field
		if (canModify && modifyPermission == null) {
			modifyPermission = new GenericPermission(object, MODIFY);
		}

		return modifyPermission;
	}

	public GenericPermission getReadPermission() {
		// lazy loading/update of transient field
		if (canRead && readPermission == null) {
			readPermission = new GenericPermission(object, READ);
		}
		return readPermission;
	}

	public Set<GenericPermission> getAllSetPermissions() {
		Set<GenericPermission> set = new HashSet<>();
		if (getCreatePermission() != null) {
			set.add(getCreatePermission());
		}
		if (getModifyPermission() != null) {
			set.add(getModifyPermission());
		}
		if (getDeletePermission() != null) {
			set.add(getDeletePermission());
		}
		if (getReadPermission() != null) {
			set.add(getReadPermission());
		}
		return set;
	}

	public void setCanRead(boolean b) {
		boolean old = canRead;
		canRead = b;
		if (old != canRead && canRead == true) {
			readPermission = new GenericPermission(object, READ);
		}
		if (canRead == false) {
			readPermission = null;
		}
	}

	public void setCanModify(boolean b) {
		boolean old = canModify;
		canModify = b;
		if (old != canModify && canModify == true) {
			modifyPermission = new GenericPermission(object, MODIFY);
		}
		if (canModify == false) {
			modifyPermission = null;
		}
	}

	public void setCanDelete(boolean b) {
		boolean old = canDelete;
		canDelete = b;
		if (old != canDelete && canDelete == true) {
			deletePermission = new GenericPermission(object, DELETE);
		}
		if (canDelete == false) {
			deletePermission = null;
		}
	}

	public void setCanCreate(boolean b) {
		boolean old = canCreate;
		canCreate = b;
		if (old != canCreate && canCreate == true) {
			createPermission = new GenericPermission(object, CREATE);
		}
		if (canRead == false) {
			createPermission = null;
		}
	}
}
