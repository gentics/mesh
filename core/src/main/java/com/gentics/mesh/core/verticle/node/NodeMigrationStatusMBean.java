package com.gentics.mesh.core.verticle.node;

/**
 * Interface for the JMX MBean for node migrations.
 */
public interface NodeMigrationStatusMBean {

	/**
	 * Name of the schema.
	 * 
	 * @return schema name
	 */
	String getName();

	/**
	 * Schema version.
	 * 
	 * @return schema version
	 */
	int getVersion();

	/**
	 * Get total number of nodes.
	 * 
	 * @return total number of nodes
	 */
	int getTotalNodes();

	/**
	 * Get number of migrated nodes.
	 * 
	 * @return number of migrated nodes
	 */
	int getNodesDone();
}
