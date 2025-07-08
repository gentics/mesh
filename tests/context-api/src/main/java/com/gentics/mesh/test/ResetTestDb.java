package com.gentics.mesh.test;

/**
 * Setting, whether the test db needs to be reset between test runs
 */
public enum ResetTestDb {
	/**
	 * Test db will always be reset between test runs
	 */
	ALWAYS,

	/**
	 * Test db will never be reset between test runs
	 */
	NEVER,

	/**
	 * Test db will only be reset between test runs, when the hash, which is set via MeshTestContext.setDbHash() is different than the previous one.
	 */
	ON_HASH_CHANGE;
}
