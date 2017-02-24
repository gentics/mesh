package com.gentics.mesh.test.context;

import static com.gentics.mesh.test.TestSize.PROJECT;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import com.gentics.mesh.test.TestSize;

@Retention(RetentionPolicy.RUNTIME)
public @interface MeshTestSetting {

	boolean useElasticsearch() default false;

	TestSize testSize() default PROJECT;

	boolean startServer() default false;

}
