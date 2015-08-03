package com.gentics.mesh.core.rest.group;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;

/**
 * Group response model.
 */
public class GroupResponse extends AbstractGenericNodeRestModel {

	private String name;

	private List<String> roles = new ArrayList<>();

	public GroupResponse() {
	}

	/**
	 * Return the name of the group.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the name of the group.
	 * 
	 * @param name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Returns a list of roles which are assigned to the group.
	 * 
	 * @return
	 */
	public List<String> getRoles() {
		return roles;
	}

}
