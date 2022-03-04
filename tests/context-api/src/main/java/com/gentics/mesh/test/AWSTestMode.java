package com.gentics.mesh.test;

/**
 * Various test modes for AWS S3 tests.
 */
public enum AWSTestMode {

	/**
	 * Run with the real AWS connection
	 */
	AWS,

	/**
	 * Run with a local minio container
	 */
	MINIO,

	/**
	 * No AWS connection setup
	 */
	NONE;

}