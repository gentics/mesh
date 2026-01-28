package com.gentics.mesh.test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.gentics.mesh.test.MeshOptionChanger.NoOptionChanger;

/**
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface MeshTestSetting {

	/**
	 * Flag which indicates whether the ES docker container should be started.
	 * 
	 * @return
	 */
	ElasticsearchTestMode elasticsearch() default ElasticsearchTestMode.NONE;

	/**
	 * Flag which indicates whether the AWS docker container should be started.
	 *
	 * @return
	 */
	AWSTestMode awsContainer() default AWSTestMode.NONE;

	/**
	 * Setting which indicates what size of test data should be created.
	 * 
	 * @return
	 */
	TestSize testSize() default TestSize.PROJECT;

	/**
	 * Flag which indicates whether the mesh http server should be started.
	 * 
	 * @return
	 */
	boolean startServer() default false;

	/**
	 * Flag which indicates whether the graph database should run in-memory mode.
	 * 
	 * @return
	 */
	boolean inMemoryDB() default true;

	/**
	 * Flag which indicates whether the graph database server should be started (only if {@link #inMemoryDB()} is false)
	 * 
	 * @return
	 */
	boolean startStorageServer() default false;

	/**
	 * Flag which indicates whether the keycloak server should be used.
	 * 
	 * @return
	 */
	boolean useKeycloak() default false;

	/**
	 * Flag which indicates that the cluster mode should be enabled.
	 * 
	 * @return
	 */
	boolean clusterMode() default false;

	/**
	 * Name of the cluster (when {@link #clusterMode()} is true)
	 * @return name of the cluster
	 */
	String clusterName() default "";

	/**
	 * Number of started instances, when {@link #clusterMode()} is true. Default is 3 instances.
	 * @return number of cluster instances
	 */
	int clusterInstances() default 3;

	/**
	 * Array of node names to be used for the instances. If more instances are created, they will get created node names.
	 * @return node names
	 */
	String[] nodeNames() default {};

	/**
	 * SSL test mode.
	 * 
	 * @return
	 */
	SSLTestMode ssl() default SSLTestMode.OFF;

	/**
	 * Flag which indicates whether the monitoring feature should be enabled.
	 * 
	 * @return
	 */
	boolean monitoring() default true;

	/**
	 * Flag which indicates that write operations should be synchronized.
	 * @return
	 */
	boolean synchronizeWrites() default false;

	/**
	 * Predefined set of options changers that can be used to alter the mesh options for the test.
	 * 
	 * @return
	 */
	MeshCoreOptionChanger optionChanger() default MeshCoreOptionChanger.NO_CHANGE;

	/**
	 * Class of the option changer to user
	 * @return
	 */
	Class<? extends MeshOptionChanger> customOptionChanger() default NoOptionChanger.class;

	/**
	 * Flag which indicates whether the database shall be reset between test runs (default is {@link ResetTestDb#ALWAYS})
	 * @return
	 */
	ResetTestDb resetBetweenTests() default ResetTestDb.ALWAYS;
}
