package com.gentics.mesh.core.rest.admin.cluster;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * REST POJO for cluster info response.
 */
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

	/**
	 * Return the node name.
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the node name.
	 * 
	 * @param name
	 * @return
	 */
	public ClusterInstanceInfo setName(String name) {
		this.name = name;
		return this;
	}

	/**
	 * Return the cluster status.
	 * 
	 * @return
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Set the cluster status.
	 * 
	 * @param status
	 * @return
	 */
	public ClusterInstanceInfo setStatus(String status) {
		this.status = status;
		return this;
	}

	/**
	 * Return the start date of the node.
	 * 
	 * @return
	 */
	public String getStartDate() {
		return startDate;
	}

	/**
	 * Set the date at which the node was started.
	 * 
	 * @param startDate
	 * @return
	 */
	public ClusterInstanceInfo setStartDate(String startDate) {
		this.startDate = startDate;
		return this;
	}

	/**
	 * Return the currently configured role of the cluster instance.
	 * 
	 * @return
	 */
	public String getRole() {
		return role;
	}

	/**
	 * Set the cluste role of the instance.
	 * 
	 * @param role
	 * @return
	 */
	public ClusterInstanceInfo setRole(String role) {
		this.role = role;
		return this;
	}
}
