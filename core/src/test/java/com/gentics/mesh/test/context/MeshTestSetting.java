package com.gentics.mesh.test.context;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface MeshTestSetting {

	boolean useElasticsearch() default false;

	boolean useTinyDataset() default true;

	boolean startServer() default false;

}
