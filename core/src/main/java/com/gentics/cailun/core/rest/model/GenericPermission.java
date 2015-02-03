package com.gentics.cailun.core.rest.model;

import org.springframework.data.neo4j.annotation.NodeEntity;


@NodeEntity
public class GenericPermission extends AbstractPersistable implements org.apache.shiro.authz.Permission  {

	private static final long serialVersionUID = 8437529635544588899L;

	private final String actionName;

	private final GenericNode targetObject;

	/**
	 * Create a new generic permission that is linked to the given target object
	 * 
	 * @param targetObject
	 * @param actionName
	 */
	public GenericPermission(GenericNode targetObject, String actionName) {
		this.targetObject = targetObject;
		this.actionName = actionName;
	}

	public String getActionName() {
		return actionName;
	}

	public GenericNode getTargetObject() {
		return targetObject;
	}

	@Override
	public boolean implies(org.apache.shiro.authz.Permission p) {
		if (!(p instanceof GenericPermission)) {
			return false;
		}
		GenericPermission pp = (GenericPermission) p;
		return pp.getTargetObject().equals(targetObject) && actionName.equals(pp.getActionName());

	}

}
