package com.gentics.mesh.core.verticle.node;

/**
 * JMX MBean for the node migrations
 */
public class NodeMigrationStatus implements NodeMigrationStatusMBean {
	protected String schemaName;

	protected int version;

	protected int totalNodes;

	protected int nodesDone = 0;

	/**
	 * Create an instance
	 * @param schemaName schema name
	 * @param version schema version
	 */
	public NodeMigrationStatus(String schemaName, int version) {
		this.schemaName = schemaName;
		this.version = version;
	}

	@Override
	public String getSchemaName() {
		return schemaName;
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public int getTotalNodes() {
		return totalNodes;
	}

	/**
	 * Set the total nubmer of nodes to migrate
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
