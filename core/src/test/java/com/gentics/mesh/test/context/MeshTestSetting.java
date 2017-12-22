package com.gentics.mesh.test.context;

import static com.gentics.mesh.test.TestSize.PROJECT;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.gentics.mesh.test.TestSize;

@Retention(RetentionPolicy.RUNTIME)
public @interface MeshTestSetting {

	boolean useElasticsearch() default false;

	/**
	 * Flag which indicates whether the ES http server should be started.
	 * 
	 * @return
	 * @deprecated Currently not supported since Netty of Vert.x 3.5.0 is not compatible with ES 
	 */
	@Deprecated
	boolean startESServer() default false;

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
	 * Flag which indicates that the cluster mode should be enabled.
	 * 
	 * @return
	 */
	boolean clusterMode() default false;
}
