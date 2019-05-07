package com.gentics.mesh.test.context;

import static com.gentics.mesh.test.TestSize.PROJECT;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.gentics.mesh.test.TestSize;

@Retention(RetentionPolicy.RUNTIME)
public @interface MeshTestSetting {

	/**
	 * Flag which indicates whether the ES docker container should be started.
	 * 
	 * @return
	 */
	boolean useElasticsearch() default false;

	/**
	 * Flag which indicates whether the ES docker container should use the ingest plugin.
	 * 
	 * @return
	 */
	boolean withIngestPlugin() default false;

	/**
	 * Flag which indicates whether the ES test container should be used. Otherwise the embedded ES will be used instead.
	 * 
	 * @return
	 */
	boolean useElasticsearchContainer() default true;

	/**
	 * Setting which indicates what size of test data should be created.
	 * 
	 * @return
	 */
	TestSize testSize() default PROJECT;

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
	 * Flag which indicates whether SSL should be used.
	 * 
	 * @return
	 */
	boolean ssl() default false;

	/**
	 * Flag which indicates whether the monitoring feature should be enabled.
	 * @return
	 */
	boolean monitoring() default true;
}
