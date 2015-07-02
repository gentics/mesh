package com.gentics.mesh.core.data.service;

import com.gentics.mesh.util.InvalidArgumentException;

public interface BasicObjectTestcases {

	void testFindAllVisible() throws InvalidArgumentException;

	void testFindAll() throws InvalidArgumentException;

	void testRootNode();

	void testFindByName();

	void testFindByUUID();

	void testRead();

	void testCreate();

	void testDelete();

	void testUpdate();

	void testReadPermission();

	void testDeletePermission();

	void testUpdatePermission();

	void testCreatePermission();

	void testTransformation();

	void testCreateDelete();

	void testCRUDPermissions();

	void testPermissionsOnObject();

}
