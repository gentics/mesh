package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class TagFamilyTest extends AbstractBasicObjectTest {
	
	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		TagFamilyRoot root = getMeshRoot().getTagFamilyRoot();
		root.findAll(getRequestUser(), new PagingInfo(1, 10));
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		TagFamilyRoot root = getMeshRoot().getTagFamilyRoot();
		List<? extends TagFamily> families = root.findAll();
		assertNotNull(families);
		assertEquals(2, families.size());

		TagFamilyRoot projectTagFamilyRoot = getProject().getTagFamilyRoot();
		assertNotNull(projectTagFamilyRoot);

		TagFamily projectTagFamily = projectTagFamilyRoot.findByName("colors");
		assertNotNull(projectTagFamily);

		assertNotNull(projectTagFamilyRoot.create("bogus"));
		assertEquals(3, projectTagFamilyRoot.findAll().size());
		assertEquals(3, root.findAll().size());
	}

	@Test
	@Override
	public void testRootNode() {

	}

	@Override
	public void testFindByName() {
		TagFamilyRoot root = getMeshRoot().getTagFamilyRoot();
		assertNotNull(root);
		assertNotNull(root.findByName("colors"));

	}

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
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testDeletePermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdatePermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreatePermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testTransformation() {
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
