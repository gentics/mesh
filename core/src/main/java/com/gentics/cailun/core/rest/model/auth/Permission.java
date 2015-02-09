package com.gentics.cailun.core.rest.model.auth;

import lombok.Data;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

import com.gentics.cailun.core.rest.model.AbstractPersistable;
import com.gentics.cailun.core.rest.model.GenericNode;

@RelationshipEntity
public class Permission extends AbstractPersistable {

	private static final long serialVersionUID = 8304718445043642942L;

	@Fetch
	@StartNode
	private Role role;

	@Fetch
	@EndNode
	private GenericNode targetNode;

	private boolean read = false;
	private boolean write = false;
	private boolean delete = false;
	private boolean create = false;

	public Permission() {
	}

	@PersistenceConstructor
	public Permission(Role role, GenericNode targetNode) {
		this.role = role;
		this.targetNode = targetNode;
	}

	public void setCreate(boolean flag) {
		this.create = flag;
	}

	public boolean isCreate() {
		return create;
	}

	public boolean isWrite() {
		return write;
	}

	public void setWrite(boolean write) {
		this.write = write;
	}

	public boolean isRead() {
		return read;
	}

	public void setRead(boolean read) {
		this.read = read;
	}

	public void setDelete(boolean delete) {
		this.delete = delete;
	}

	public boolean isDelete() {
		return delete;
	}

}
