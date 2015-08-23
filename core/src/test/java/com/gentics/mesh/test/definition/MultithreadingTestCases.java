package com.gentics.mesh.test.definition;

public interface MultithreadingTestCases {

	/**
	 * Test multithreaded update calls and block in Trx.commit() to check for collision issues.
	 * 
	 * @throws InterruptedException
	 */
	void testUpdateNodeMultithreaded() throws InterruptedException;

	/**
	 * Test multithreaded read calls and block in Trx.commit() to check for collision issues.
	 * 
	 * @throws InterruptedException
	 */
	void testReadNodeByUUIDMultithreaded() throws InterruptedException;

	void testDeleteNodeByUUIDMultithreaded() throws InterruptedException;

	void testCreateNodeMultithreaded() throws InterruptedException;

	/**
	 * Test multithreaded read calls which exceed the worker pool size.
	 * 
	 * @throws InterruptedException
	 */
	void testReadNodeByUUIDMultithreadedNonBlocking() throws InterruptedException;

}
