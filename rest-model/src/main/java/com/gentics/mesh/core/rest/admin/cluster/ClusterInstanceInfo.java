package com.gentics.mesh.core.rest.admin.cluster;

import com.gentics.mesh.core.rest.common.RestModel;

public class ClusterInstanceInfo implements RestModel {

	private String address;
	private String name;
	private String status;
	private String startDate;
	private String role;

	/**
	 * Return the cluster address.
	 * 
	 * @return
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * Set the instance address
	 * 
	 * @param address
	 * @return Fluent API
	 */
	public ClusterInstanceInfo setAddress(String address) {
		this.address = address;
		return this;
	}

	public String getName() {
		return name;
	}

	public ClusterInstanceInfo setName(String name) {
		this.name = name;
		return this;
	}

	public ClusterInstanceInfo setStatus(String status) {
		this.status = status;
		return this;
	}

	public String getStatus() {
		return status;
	}

	public ClusterInstanceInfo setStartDate(String startDate) {
		this.startDate = startDate;
		return this;
	}

	public String getStartDate() {
		return startDate;
	}

	public String getRole() {
		return role;
	}

	public ClusterInstanceInfo setRole(String role) {
		this.role = role;
		return this;
	}
}
