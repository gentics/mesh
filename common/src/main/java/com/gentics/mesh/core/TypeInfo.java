package com.gentics.mesh.core;

/**
 * Container for type specific meta information.
 */
public class TypeInfo {

	private String type;
	private String onCreatedAddress;
	private String onUpdatedAddress;
	private String onDeletedAddress;

	public TypeInfo(String type, String onCreatedAddress, String onUpdatedAddress, String onDeletedAddress) {
		this.type = type;
		this.onCreatedAddress = onCreatedAddress;
		this.onUpdatedAddress = onUpdatedAddress;
		this.onDeletedAddress = onDeletedAddress;
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
	 * Return the onCreated eventbus address.
	 * 
	 * @return
	 */
	public String getOnCreatedAddress() {
		return onCreatedAddress;
	}

	/**
	 * Return the onDeleted eventbus address.
	 * 
	 * @return
	 */
	public String getOnDeletedAddress() {
		return onDeletedAddress;
	}

	/**
	 * Return the onUpdated eventbus address.
	 * 
	 * @return
	 */
	public String getOnUpdatedAddress() {
		return onUpdatedAddress;
	}
}
