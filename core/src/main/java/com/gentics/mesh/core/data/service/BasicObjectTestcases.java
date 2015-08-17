package com.gentics.mesh.core.data.service;

import java.io.IOException;

import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.util.InvalidArgumentException;

public interface BasicObjectTestcases {

	void testFindAllVisible() throws InvalidArgumentException;

	void testFindAll() throws InvalidArgumentException;

	void testRootNode() throws MeshSchemaException;

	void testFindByName() throws IOException;

	void testFindByUUID() throws InterruptedException;

	void testRead() throws IOException;

	void testCreate() throws IOException;

	void testDelete() throws InterruptedException;

	void testUpdate() throws IOException;

	void testReadPermission() throws MeshSchemaException;

	void testDeletePermission() throws MeshSchemaException;

	void testUpdatePermission() throws MeshSchemaException;

	void testCreatePermission() throws MeshSchemaException;

	void testTransformation() throws IOException, InterruptedException;

	void testCreateDelete() throws InterruptedException, MeshSchemaException;

	void testCRUDPermissions() throws MeshSchemaException;

}
