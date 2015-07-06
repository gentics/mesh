package com.gentics.mesh.core;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class MicroschemaTest extends AbstractBasicObjectTest {

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testRootNode() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testFindByName() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testFindByUUID() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testRead() throws IOException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreate() throws IOException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testDelete() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdate() {
		// TODO Auto-generated method stub

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

	}

	@Test
	@Override
	public void testCreateDelete() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCRUDPermissions() {
		// TODO Auto-generated method stub

	}

}
