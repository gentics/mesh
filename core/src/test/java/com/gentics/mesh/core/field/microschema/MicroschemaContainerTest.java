package com.gentics.mesh.core.field.microschema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.MicroschemaContainerRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.json.MeshJsonException;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.ext.web.RoutingContext;

public class MicroschemaContainerTest extends AbstractBasicObjectTest {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		MicroschemaContainer vcard = microschemaContainer("vcard");
		MicroschemaReference reference = vcard.transformToReference();
		assertNotNull(reference);
		assertEquals("vcard", reference.getName());
		assertEquals(vcard.getUuid(), reference.getUuid());
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		MeshAuthUser requestUser = ac.getUser();

		int expectedMicroschemaContainers = microschemaContainers().size();

		for (int i = 1; i <= expectedMicroschemaContainers + 1; i++) {
			PageImpl<? extends MicroschemaContainer> page = boot.microschemaContainerRoot().findAll(requestUser, new PagingParameter(1, i));

			assertEquals(microschemaContainers().size(), page.getTotalElements());
			assertEquals(Math.min(expectedMicroschemaContainers, i), page.getSize());
		}
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testRootNode() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testFindByName() {
		String invalidName = "thereIsNoMicroschemaWithThisName";

		for (String name : microschemaContainers().keySet()) {
			MicroschemaContainer container = boot.microschemaContainerRoot().findByName(name).toBlocking().single();
			assertNotNull("Could not find microschema container for name " + name, container);
			Microschema microschema = container.getLatestVersion().getSchema();
			assertNotNull("Container for microschema " + name + " did not contain a microschema", microschema);
			assertEquals("Check microschema name", name, microschema.getName());
		}

		assertNull("Must not find microschema with name " + invalidName,
				boot.microschemaContainerRoot().findByName(invalidName).toBlocking().single());
	}

	@Test
	@Override
	public void testFindByUUID() {
		String invalidUUID = UUIDUtil.randomUUID();

		MicroschemaContainerRoot root = boot.microschemaContainerRoot();
		for (MicroschemaContainer container : microschemaContainers().values()) {
			String uuid = container.getUuid();
			assertNotNull("Could not find microschema with uuid " + uuid, root.findByUuid(uuid).toBlocking().single());
		}

		assertNull("Must not find microschema with uuid " + invalidUUID, root.findByUuid(invalidUUID).toBlocking().single());
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testRead() throws IOException {
		fail("Not yet implemented");
	}

	@Test
	public void testRoot() {
		MicroschemaContainer vcard = microschemaContainer("vcard");
		assertNotNull(vcard.getRoot());
	}

	@Test
	@Override
	public void testCreate() throws IOException {
		Microschema schema = new MicroschemaModel();
		schema.setName("test");
		MicroschemaContainer container = MeshRoot.getInstance().getMicroschemaContainerRoot().create(schema, user());
		assertNotNull("The container was not created.", container);
		assertNotNull("The container schema was not set", container.getLatestVersion().getSchema());
		assertEquals("The creator was not set.", user().getUuid(), container.getCreator().getUuid());
	}

	/**
	 * Assert that the schema version is in sync with its rest model.
	 */
	@Test
	public void testVersionSync() {
		assertNotNull(microschemaContainer("vcard"));
		assertEquals("The microschema container and schema rest model version must always be in sync",
				microschemaContainer("vcard").getLatestVersion().getVersion(),
				microschemaContainer("vcard").getLatestVersion().getSchema().getVersion());

	}

	@Test
	@Override
	public void testDelete() throws MeshJsonException {
		Microschema schema = new MicroschemaModel();
		schema.setName("test");
		MicroschemaContainer container = MeshRoot.getInstance().getMicroschemaContainerRoot().create(schema, user());
		assertNotNull(MeshRoot.getInstance().getMicroschemaContainerRoot().findByName("test").toBlocking().single());
		SearchQueueBatch batch = createBatch();
		container.delete(batch);
		assertNull(MeshRoot.getInstance().getMicroschemaContainerRoot().findByName("test").toBlocking().single());
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testUpdate() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testReadPermission() throws MeshJsonException {
		Microschema microschema = new MicroschemaModel();
		microschema.setName("someNewMicroschema");
		MicroschemaContainer microschemaContainer = meshRoot().getMicroschemaContainerRoot().create(microschema, user());
		testPermission(GraphPermission.READ_PERM, microschemaContainer);
	}

	@Test
	@Override
	public void testDeletePermission() throws MeshJsonException {
		Microschema microschema = new MicroschemaModel();
		microschema.setName("someNewMicroschema");
		MicroschemaContainer microschemaContainer = meshRoot().getMicroschemaContainerRoot().create(microschema, user());
		testPermission(GraphPermission.DELETE_PERM, microschemaContainer);

	}

	@Test
	@Override
	public void testUpdatePermission() throws MeshJsonException {
		Microschema microschema = new MicroschemaModel();
		microschema.setName("someNewMicroschema");
		MicroschemaContainer microschemaContainer = meshRoot().getMicroschemaContainerRoot().create(microschema, user());
		testPermission(GraphPermission.UPDATE_PERM, microschemaContainer);
	}

	@Test
	@Override
	public void testCreatePermission() throws MeshJsonException {
		Microschema microschema = new MicroschemaModel();
		microschema.setName("someNewMicroschema");
		MicroschemaContainer microschemaContainer = meshRoot().getMicroschemaContainerRoot().create(microschema, user());
		testPermission(GraphPermission.CREATE_PERM, microschemaContainer);
	}

	@Test
	@Override
	public void testTransformation() throws IOException {
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		MicroschemaContainer vcard = microschemaContainer("vcard");
		Microschema schema = vcard.transformToRest(ac, 0, "en").toBlocking().single();
		assertEquals(vcard.getUuid(), schema.getUuid());
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testCreateDelete() {
		// TODO Auto-generated method stub
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCRUDPermissions() throws MeshJsonException {
		MicroschemaContainerRoot root = meshRoot().getMicroschemaContainerRoot();

		Microschema microschema = new MicroschemaModel();
		microschema.setName("someNewMicroschema");
		MicroschemaContainer container = root.create(microschema, user());

		assertFalse(role().hasPermission(GraphPermission.CREATE_PERM, container));
		getRequestUser().addCRUDPermissionOnRole(meshRoot().getMicroschemaContainerRoot(), GraphPermission.CREATE_PERM, container);
		assertTrue("The addCRUDPermissionOnRole method should add the needed permissions on the new microschema container.",
				role().hasPermission(GraphPermission.CREATE_PERM, container));
	}
}
