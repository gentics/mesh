package com.gentics.vertx.cailun.perm.model;

import lombok.NoArgsConstructor;

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
// @Data
// @EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
@NoArgsConstructor
public class Permission extends AbstractPersistable implements org.apache.shiro.authz.Permission {

	private static final long serialVersionUID = 1564260534291371364L;

	@Fetch
	@StartNode
	private Role role;

	@Fetch
	@EndNode
	private GenericNode object;

	@Fetch
	private boolean canRead = false;

	@Fetch
	private boolean canWrite = false;

	@Fetch
	private boolean canDelete = false;

	@Fetch
	private boolean canCreate = false;

	public Permission(Role role, GenericNode object) {
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

	public boolean isCanCreate() {
		return canCreate;
	}

	public void setCanCreate(boolean canCreate) {
		this.canCreate = canCreate;
	}

	public boolean isCanDelete() {
		return canDelete;
	}

	public void setCanDelete(boolean canDelete) {
		this.canDelete = canDelete;
	}

	public boolean isCanWrite() {
		return canWrite;
	}

	public void setCanWrite(boolean canWrite) {
		this.canWrite = canWrite;
	}

	public boolean isCanRead() {
		return canRead;
	}

	public void setCanRead(boolean canRead) {
		this.canRead = canRead;
	}

	@Override
	public boolean implies(org.apache.shiro.authz.Permission p) {
		// TODO Auto-generated method stub
		return false;
	}
}
