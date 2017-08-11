package com.gentics.mesh.core;

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

	public String getType() {
		return type;
	}

	public String getOnCreatedAddress() {
		return onCreatedAddress;
	}

	public String getOnDeletedAddress() {
		return onDeletedAddress;
	}

	public String getOnUpdatedAddress() {
		return onUpdatedAddress;
	}
}
