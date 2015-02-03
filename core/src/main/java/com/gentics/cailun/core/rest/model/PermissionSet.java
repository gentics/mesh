package com.gentics.cailun.core.rest.model;

import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

/**
 * The permission object is an element that is used to form the ACL domain in the graph.
 * 
 * @author johannes2
 *
 */
@RelationshipEntity
public class PermissionSet extends AbstractPersistable {

	private static final long serialVersionUID = 1564260534291371364L;

	// public static final String READ = "read";
	// public static final String DELETE = "delete";
	// public static final String CREATE = "create";
	// public static final String MODIFY = "modify";

	public static final String RELATION_KEYWORD = "HAS_PERMISSIONSET";

	@Fetch
	@StartNode
	private Role role;

	@Fetch
	@EndNode
	private GenericNode object;

	@Fetch
	Set<GenericPermission> permissions = new HashSet<>();

	// @Fetch
	// private boolean canRead = false;
	//
	// @Transient
	// private GenericPermission readPermission = null;
	//
	// @Fetch
	// private boolean canModify = false;
	//
	// @Transient
	// private GenericPermission modifyPermission = null;
	//
	// @Fetch
	// private boolean canDelete = false;
	//
	// @Transient
	// private GenericPermission deletePermission = null;
	//
	// @Fetch
	// private boolean canCreate = false;
	//
	// @Transient
	// private GenericPermission createPermission = null;

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

	public void addPermission(GenericPermission permission) {
		this.permissions.add(permission);
	}
}
