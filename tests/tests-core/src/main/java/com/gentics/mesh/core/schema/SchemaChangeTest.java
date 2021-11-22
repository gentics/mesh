package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.function.Function;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.schema.HibFieldSchemaElement;
import com.gentics.mesh.core.data.schema.HibFieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibRemoveFieldChange;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaChange;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.UUIDUtil;

@MeshTestSetting(testSize = FULL, startServer = true)
public class SchemaChangeTest extends AbstractMeshTest {

	@Test
	public void testDomainModel() {
		try (Tx tx = tx()) {
			CommonTx ctx = (CommonTx) tx;
			PersistingSchemaDao schemaDao = ctx.schemaDao();
			HibSchema container = schemaDao.createPersisted(UUIDUtil.randomUUID());

			HibSchemaVersion versionA = container.getLatestVersion();
			HibSchemaVersion versionB = createSchemaVersion(ctx, container);
			HibSchemaVersion versionC = createSchemaVersion(ctx, container);

			HibRemoveFieldChange change = (HibRemoveFieldChange) schemaDao.createPersistedChange(versionA, SchemaChangeOperation.REMOVEFIELD);
			// Not true anymore, since version initialization is now the part of container initialization, as no container can exist without a version.
			//assertNull("Initially no version should have been set", container.getLatestVersion());
			container.setLatestVersion(versionA);
			assertEquals("The uuid of the latest version did not match to versionA's uuid.", versionA.getUuid(),
				container.getLatestVersion().getUuid());

			assertNull("The previous change should be null since we did not link it to any schema version.", versionA.getPreviousChange());
			assertNull("The next change should be null since we did not link it to any schema version.", versionA.getNextChange());

			versionA.setNextChange(change);
			assertNotNull("The next change was not found but we linked it to the schema container.", versionA.getNextChange());

			versionA.setPreviousChange(change);
			assertNotNull("The previous change was not found but we linked it to the schema container.", versionA.getPreviousChange());

			assertNull("The next version was not yet set and thus should be null but it was not.", versionB.getNextVersion());
			versionB.setNextVersion(versionC);
			assertNotNull(versionB.getNextVersion());

			assertNull("The next version was not yet set and thus should be null but it was not.", versionB.getPreviousVersion());
			versionB.setPreviousVersion(versionA);
			assertNotNull(versionB.getPreviousVersion());
		}
	}

	@Test
	public void testMicroschemaChanges() {
		try (Tx tx = tx()) {
			CommonTx ctx = (CommonTx) tx;
			HibMicroschema container = createMicroschema(ctx);
			HibMicroschemaVersion versionA = container.getLatestVersion();
			HibMicroschemaVersion versionB = createMicroschemaVersion(ctx, container);
			container.setLatestVersion(versionB);
			HibSchemaChange<?> oldChange = chainChanges(versionA, versionB,
					version ->  ctx.microschemaDao().createPersistedChange((HibMicroschemaVersion) version, SchemaChangeOperation.REMOVEFIELD));
			validate(container, versionA, versionB, oldChange, container1 -> createMicroschemaVersion(CommonTx.get(), (HibMicroschema) container1));
		}
	}

	@Test
	public void testChangeChain() {
		try (Tx tx = tx()) {
			CommonTx ctx = (CommonTx) tx;
			HibSchema container = createSchema(ctx);
			HibSchemaVersion versionA = container.getLatestVersion();
			HibSchemaVersion versionB = createSchemaVersion(ctx, container);
			container.setLatestVersion(versionA);
			HibSchemaChange<?> oldChange = chainChanges(versionA, versionB, 
					version ->  ctx.schemaDao().createPersistedChange((HibSchemaVersion) version, SchemaChangeOperation.REMOVEFIELD));
			validate(container, versionA, versionB, oldChange, container1 -> createSchemaVersion(CommonTx.get(), (HibSchema) container1));
		}
	}

	/**
	 * Chain multiple changes in between the given versions.
	 * 
	 * @param versionA
	 * @param versionB
	 * @return
	 */
	private HibSchemaChange<?> chainChanges(HibFieldSchemaVersionElement versionA, HibFieldSchemaVersionElement versionB,
			Function<HibFieldSchemaVersionElement, HibSchemaChange<?>> schemaChangeProvider) {
		HibSchemaChange<?> oldChange = null;
		for (int i = 0; i < 3; i++) {
			HibSchemaChange<?> change = schemaChangeProvider.apply(versionB);
			if (oldChange == null) {
				oldChange = change;
				assertNull("The change has not yet been connected to any schema", oldChange.getPreviousContainerVersion());
				versionA.setNextChange(oldChange);
				assertNotNull("The change has been connected to the schema container and thus the connection should be loadable",
					oldChange.getPreviousContainerVersion());
			} else {
				oldChange.setNextChange(change);
				oldChange = change;
			}
		}
		return oldChange;
	}

	/**
	 * Validate the chain of changes in between the containers.
	 * 
	 * @param container
	 * @param versionA
	 * @param versionB
	 * @param oldChange
	 */
	private void validate(HibFieldSchemaElement container, HibFieldSchemaVersionElement versionA, HibFieldSchemaVersionElement versionB,
		HibSchemaChange<?> oldChange, Function<HibFieldSchemaElement, HibFieldSchemaVersionElement> schemaVersionProvider) {
		versionA.setNextVersion(versionB);
		assertNull(oldChange.getNextContainerVersion());
		oldChange.setNextSchemaContainerVersion(versionB);
		assertNotNull(oldChange.getNextContainerVersion());
		assertNotNull("The containerA should have a next change", versionA.getNextChange());
		assertNull("The container should not have any previous change", versionA.getPreviousChange());
		HibSchemaChange<?> secondLastChange = versionA.getNextChange().getNextChange();
		HibSchemaChange<?> lastChange = secondLastChange.getNextChange();
		assertNull("This is the last change in the chain and thus no next change should be set", lastChange.getNextChange());
		assertEquals("The previous change from the last change should be the second last change.", secondLastChange.getUuid(),
			lastChange.getPreviousChange().getUuid());

		assertEquals("The last change should be connected to the containerB but it was not.", versionB.getUuid(),
			lastChange.getNextContainerVersion().getUuid());
		assertNull("The change has no from schema container because it it part of a chain of changes.", lastChange.getPreviousContainerVersion());

		assertEquals("The previous change of the schema that was connected to the last change did not match the last change.", lastChange.getUuid(),
			lastChange.getNextContainerVersion().getPreviousChange().getUuid());

		// Link the chain root to another schema container instead.
		HibFieldSchemaVersionElement versionC = schemaVersionProvider.apply(container);
		HibSchemaChange<?> firstChange = versionA.getNextChange();
		firstChange.setPreviousContainerVersion(versionC);
		assertNotEquals("The first change should no longer be connected to containerA", versionA.getUuid(),
			firstChange.getPreviousContainerVersion().getUuid());
		assertEquals("The chain of changes should now be connected to container version C", versionC.getUuid(),
			firstChange.getPreviousContainerVersion().getUuid());

		// Check next version
		assertNotNull("Version A should have a next version.", versionA.getNextVersion());
		assertEquals("Version B should be the next version of version A.", versionB.getUuid(), versionA.getNextVersion().getUuid());

		// Check latest version
		container.setLatestVersion(versionB);
		HibFieldSchemaVersionElement latest = container.getLatestVersion();
		assertNotNull("There should always be a latest version", latest);
		assertEquals("Version B should represent the latest version but it did not", versionB.getUuid(), latest.getUuid());

	}

}
