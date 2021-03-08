package com.gentics.mesh.core.data.service;

/**
 * Common definition of testcases for basic core objects.
 */
public interface BasicObjectTestcases {

	void testTransformToReference() throws Exception;

	void testFindAllVisible() throws Exception;

	void testFindAll() throws Exception;

	void testRootNode() throws Exception;

	void testFindByName() throws Exception;

	void testFindByUUID() throws Exception;

	void testRead() throws Exception;

	void testCreate() throws Exception;

	void testDelete() throws Exception;

	void testUpdate() throws Exception;

	void testReadPermission() throws Exception;

	void testDeletePermission() throws Exception;

	void testUpdatePermission() throws Exception;

	void testCreatePermission() throws Exception;

	void testTransformation() throws Exception;

	void testCreateDelete() throws Exception;

	/**
	 * Test whether CRUD permissions are correctly inherited.
	 * 
	 * @throws Exception
	 */
	void testCRUDPermissions() throws Exception;

}
