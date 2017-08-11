package com.gentics.mesh.core.verticle.migration;

/**
 * JMX MBean for the node migrations
 */
public class NodeMigrationStatus implements NodeMigrationStatusMBean {

	protected MigrationType type;

	protected String name;

	protected String version;

	protected int totalNodes;

	protected int nodesDone = 0;

	/**
	 * Create an instance
	 * @param name schema name
	 * @param version schema version
	 * @param type type
	 */
	public NodeMigrationStatus(String name, String version, MigrationType type) {
		this.name = name;
		this.version = version;
		this.type = type;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public int getTotalNodes() {
		return totalNodes;
	}

	/**
	 * Set the total number of nodes to migrate
	 * @param totalNodes total number
	 */
	public void setTotalNodes(int totalNodes) {
		this.totalNodes = totalNodes;
	}

	@Override
	public int getNodesDone() {
		return nodesDone;
	}

	/**
	 * Increase the number of nodes done
	 */
	public void incNodesDone() {
		nodesDone++;
	}


}
