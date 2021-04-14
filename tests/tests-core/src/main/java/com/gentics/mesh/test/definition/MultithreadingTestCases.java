package com.gentics.mesh.test.definition;

public interface MultithreadingTestCases {

	/**
	 * Test multithreaded update calls and block in Trx.commit() to check for collision issues.
	 * 
	 * @throws InterruptedException
	 */
	void testUpdateMultithreaded() throws Exception;

	/**
	 * Test multithreaded read calls and block in Trx.commit() to check for collision issues.
	 * 
	 * @throws InterruptedException
	 */
	void testReadByUuidMultithreaded() throws Exception;

	void testDeleteByUUIDMultithreaded() throws Exception;

	void testCreateMultithreaded() throws Exception;

	/**
	 * Test multithreaded read calls which exceed the worker pool size.
	 * 
	 * @throws InterruptedException
	 */
	void testReadByUuidMultithreadedNonBlocking() throws Exception;

}
