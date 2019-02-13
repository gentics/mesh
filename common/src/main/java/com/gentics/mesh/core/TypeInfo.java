package com.gentics.mesh.core;

import com.gentics.mesh.core.rest.MeshEvent;

/**
 * Container for type specific meta information.
 */
public class TypeInfo {

	private String type;
	private MeshEvent onCreated;
	private MeshEvent onUpdated;
	private MeshEvent onDeleted;

	public TypeInfo(String type, MeshEvent onCreated, MeshEvent onUpdated, MeshEvent onDeleted) {
		this.type = type;
		this.onCreated = onCreated;
		this.onUpdated = onUpdated;
		this.onDeleted = onDeleted;
	}

	/**
	 * Return the type identifier.
	 * 
	 * @return
	 */
	public String getType() {
		return type;
	}

	/**
	 * Return the onCreated eventbus event.
	 * 
	 * @return
	 */
	public MeshEvent getOnCreated() {
		return onCreated;
	}

	/**
	 * Return the onDeleted eventbus event.
	 * 
	 * @return
	 */
	public MeshEvent getOnDeleted() {
		return onDeleted;
	}

	/**
	 * Return the onUpdated eventbus event.
	 * 
	 * @return
	 */
	public MeshEvent getOnUpdated() {
		return onUpdated;
	}
}
