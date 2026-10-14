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
	 * Run with a local RustFS container
	 */
	RUSTFS,

	/**
	 * No AWS connection setup
	 */
	NONE;

}