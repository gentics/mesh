package com.gentics.mesh.core;

import static org.junit.Assert.*;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class MicroschemaTest extends AbstractBasicObjectTest {

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testRootNode() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testFindByName() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testFindByUUID() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testRead() throws IOException {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCreate() throws IOException {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testDelete() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testUpdate() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testReadPermission() {
		MicroschemaContainer microschema = getMeshRoot().getMicroschemaContainerRoot().create("someNewContainer");
		testPermission(Permission.READ_PERM, microschema);
	}

	@Test
	@Override
	public void testDeletePermission() {
		MicroschemaContainer microschema = getMeshRoot().getMicroschemaContainerRoot().create("someNewContainer");
		testPermission(Permission.DELETE_PERM, microschema);

	}

	@Test
	@Override
	public void testUpdatePermission() {
		MicroschemaContainer microschema = getMeshRoot().getMicroschemaContainerRoot().create("someNewContainer");
		testPermission(Permission.UPDATE_PERM, microschema);
	}

	@Test
	@Override
	public void testCreatePermission() {
		MicroschemaContainer microschema = getMeshRoot().getMicroschemaContainerRoot().create("someNewContainer");
		testPermission(Permission.CREATE_PERM, microschema);
	}

	@Test
	@Override
	public void testTransformation() throws IOException {
		// TODO Auto-generated method stub
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCreateDelete() {
		// TODO Auto-generated method stub
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		MicroschemaContainerRoot root = getMeshRoot().getMicroschemaContainerRoot();
		MicroschemaContainer container = root.create("newContainer");
		assertFalse(getUser().hasPermission(container, Permission.CREATE_PERM));
		getUser().addCRUDPermissionOnRole(root, Permission.CREATE_PERM, container);
		assertTrue(getUser().hasPermission(container, Permission.CREATE_PERM));
	}

}
