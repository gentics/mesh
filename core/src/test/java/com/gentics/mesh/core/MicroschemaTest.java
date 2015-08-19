package com.gentics.mesh.core;

import static com.gentics.mesh.util.VerticleHelper.getUser;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class MicroschemaTest extends AbstractBasicObjectTest {
	
	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		fail("Not yet implemented");
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {

		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = getUser(rc);

		Page<? extends MicroschemaContainer> page = boot.microschemaContainerRoot().findAll(requestUser, new PagingInfo(1, 10));

		assertEquals(data().getNodeCount(), page.getTotalElements());
		assertEquals(10, page.getSize());

		page = boot.microschemaContainerRoot().findAll(requestUser, new PagingInfo(1, 15));
		assertEquals(data().getNodeCount(), page.getTotalElements());
		assertEquals(15, page.getSize());
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testRootNode() {
		fail("Not yet implemented");
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testFindByName() {
		fail("Not yet implemented");
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testFindByUUID() {
		fail("Not yet implemented");
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testRead() throws IOException {
		fail("Not yet implemented");
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testCreate() throws IOException {
		fail("Not yet implemented");
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testDelete() {
		fail("Not yet implemented");
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testUpdate() {
		fail("Not yet implemented");
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testReadPermission() {
		MicroschemaContainer microschema = meshRoot().getMicroschemaContainerRoot().create("someNewContainer", user());
		testPermission(GraphPermission.READ_PERM, microschema);
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testDeletePermission() {
		MicroschemaContainer microschema = meshRoot().getMicroschemaContainerRoot().create("someNewContainer", user());
		testPermission(GraphPermission.DELETE_PERM, microschema);

	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testUpdatePermission() {
		MicroschemaContainer microschema = meshRoot().getMicroschemaContainerRoot().create("someNewContainer", user());
		testPermission(GraphPermission.UPDATE_PERM, microschema);
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testCreatePermission() {
		MicroschemaContainer microschema = meshRoot().getMicroschemaContainerRoot().create("someNewContainer", user());
		testPermission(GraphPermission.CREATE_PERM, microschema);
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testTransformation() throws IOException {
		// TODO Auto-generated method stub
		fail("Not yet implemented");
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testCreateDelete() {
		// TODO Auto-generated method stub
		fail("Not yet implemented");
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testCRUDPermissions() {
		MicroschemaContainerRoot root = meshRoot().getMicroschemaContainerRoot();
		MicroschemaContainer container = root.create("newContainer", user());
		assertFalse(user().hasPermission(container, GraphPermission.CREATE_PERM));
		user().addCRUDPermissionOnRole(root, GraphPermission.CREATE_PERM, container);
		assertTrue(user().hasPermission(container, GraphPermission.CREATE_PERM));
	}

}
