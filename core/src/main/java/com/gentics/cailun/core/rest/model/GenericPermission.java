package com.gentics.cailun.core.rest.model;


public class GenericPermission implements org.apache.shiro.authz.Permission {

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
