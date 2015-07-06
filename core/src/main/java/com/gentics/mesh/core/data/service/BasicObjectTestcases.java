package com.gentics.mesh.core.data.service;

import java.io.IOException;

import com.gentics.mesh.util.InvalidArgumentException;

public interface BasicObjectTestcases {

	void testFindAllVisible() throws InvalidArgumentException;

	void testFindAll() throws InvalidArgumentException;

	void testRootNode();

	void testFindByName() throws IOException;

	void testFindByUUID();

	void testRead() throws IOException;

	void testCreate() throws IOException;

	void testDelete();

	void testUpdate() throws IOException;

	void testReadPermission();

	void testDeletePermission();

	void testUpdatePermission();

	void testCreatePermission();

	void testTransformation() throws IOException;

	void testCreateDelete();

	void testCRUDPermissions();

}
