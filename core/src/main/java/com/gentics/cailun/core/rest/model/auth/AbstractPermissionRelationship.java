package com.gentics.cailun.core.rest.model.auth;

import lombok.NoArgsConstructor;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

import com.gentics.cailun.core.rest.model.AbstractPersistable;
import com.gentics.cailun.core.rest.model.GenericNode;

/**
 * The permission object is an element that is used to form the ACL domain in the graph.
 * 
 * @author johannes2
 *
 */
@RelationshipEntity
@NoArgsConstructor
public abstract class AbstractPermissionRelationship extends AbstractPersistable implements org.apache.shiro.authz.Permission {

	private static final long serialVersionUID = 8304718445043642942L;

	public static final String RELATION_KEYWORD = "HAS_PERMISSIONSET";

	@Fetch
	@StartNode
	private Role role;

	@Fetch
	@EndNode
	private GenericNode object;

	@PersistenceConstructor
	public AbstractPermissionRelationship(Role role, GenericNode object) {
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

}
