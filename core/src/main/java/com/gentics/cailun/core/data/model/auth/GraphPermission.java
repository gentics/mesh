package com.gentics.cailun.core.data.model.auth;

import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;
import org.springframework.data.neo4j.fieldaccess.DynamicProperties;
import org.springframework.data.neo4j.fieldaccess.DynamicPropertiesContainer;

import com.gentics.cailun.core.data.model.generic.AbstractPersistable;

@RelationshipEntity
public class GraphPermission extends AbstractPersistable implements org.apache.shiro.authz.Permission {

	private static final long serialVersionUID = 8304718445043642942L;

	@StartNode
	private Role role;

	@EndNode
	private AbstractPersistable targetNode;

	DynamicProperties permissions = new DynamicPropertiesContainer();

	protected GraphPermission() {
	}

	@PersistenceConstructor
	public GraphPermission(Role role, AbstractPersistable targetNode) {
		this.role = role;
		this.targetNode = targetNode;
	}

	public void grant(PermissionType type) {
		permissions.setProperty(type.getPropertyName(), true);
	}

	public void revoke(PermissionType type) {
		permissions.setProperty(type.getPropertyName(), false);
	}

	public boolean isPermitted(PermissionType type) {
		return (boolean) permissions.getProperty(type.getPropertyName(), false);
	}

	/**
	 * Returns the permission properties for this relationship.
	 * 
	 * @return
	 */
	public DynamicProperties getPermissions() {
		return permissions;
	}

	@Override
	public boolean implies(org.apache.shiro.authz.Permission p) {
		return false;
	}

	public Role getRole() {
		return role;
	}

	public AbstractPersistable getTargetNode() {
		return targetNode;
	}

}
