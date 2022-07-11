package com.gentics.mesh.test.context;

/**
 * Various test modes for AWS S3 tests.
 */
public enum AWSTestMode {

	/**
	 * Run with a local minio container
	 */
	MINIO,

	/**
	 * Run with the real AWS connection
	 */
	AWS;

}