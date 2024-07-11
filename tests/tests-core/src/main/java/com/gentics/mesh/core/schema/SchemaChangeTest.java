package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.function.Function;

import org.junit.Test;

import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.schema.FieldSchemaElement;
import com.gentics.mesh.core.data.schema.FieldSchemaVersionElement;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.MicroschemaVersion;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaVersion;
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
			Schema container = schemaDao.createPersisted(UUIDUtil.randomUUID(), s -> {
				s.setName(s.getUuid());
			});

			SchemaVersion versionA = createSchemaVersion(ctx, container, v -> {
				container.setLatestVersion(v);
			});
			SchemaVersion versionB = createSchemaVersion(ctx, container, v -> {});
			SchemaVersion versionC = createSchemaVersion(ctx, container, v -> {});

			RemoveFieldChange change = (RemoveFieldChange) schemaDao.createPersistedChange(versionA, SchemaChangeOperation.REMOVEFIELD);
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
			Microschema container = createMicroschema(ctx);
			MicroschemaVersion versionA = createMicroschemaVersion(ctx, container, v -> {
				container.setLatestVersion(v);
			});
			MicroschemaVersion versionB = createMicroschemaVersion(ctx, container, v -> {});
			container.setLatestVersion(versionB);
			SchemaChange<?> oldChange = chainChanges(versionA, versionB,
					version ->  ctx.microschemaDao().createPersistedChange((MicroschemaVersion) version, SchemaChangeOperation.REMOVEFIELD));
			validate(container, versionA, versionB, oldChange, container1 -> createMicroschemaVersion(CommonTx.get(), (Microschema) container1, v -> {}));
		}
	}

	@Test
	public void testChangeChain() {
		try (Tx tx = tx()) {
			CommonTx ctx = (CommonTx) tx;
			Schema container = createSchema(ctx);
			SchemaVersion versionA = createSchemaVersion(ctx, container, v -> {
				container.setLatestVersion(v);
			});
			SchemaVersion versionB = createSchemaVersion(ctx, container, v -> {});
			container.setLatestVersion(versionA);
			SchemaChange<?> oldChange = chainChanges(versionA, versionB, 
					version ->  ctx.schemaDao().createPersistedChange((SchemaVersion) version, SchemaChangeOperation.REMOVEFIELD));
			validate(container, versionA, versionB, oldChange, container1 -> createSchemaVersion(CommonTx.get(), (Schema) container1, v -> {}));
		}
	}

	/**
	 * Chain multiple changes in between the given versions.
	 * 
	 * @param versionA
	 * @param versionB
	 * @return
	 */
	private SchemaChange<?> chainChanges(FieldSchemaVersionElement versionA, FieldSchemaVersionElement versionB,
			Function<FieldSchemaVersionElement, SchemaChange<?>> schemaChangeProvider) {
		SchemaChange<?> oldChange = null;
		for (int i = 0; i < 3; i++) {
			SchemaChange<?> change = schemaChangeProvider.apply(versionB);
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
	private void validate(FieldSchemaElement container, FieldSchemaVersionElement versionA, FieldSchemaVersionElement versionB,
		SchemaChange<?> oldChange, Function<FieldSchemaElement, FieldSchemaVersionElement> schemaVersionProvider) {
		versionA.setNextVersion(versionB);
		assertNull(oldChange.getNextContainerVersion());
		oldChange.setNextSchemaContainerVersion(versionB);
		assertNotNull(oldChange.getNextContainerVersion());
		assertNotNull("The containerA should have a next change", versionA.getNextChange());
		assertNull("The container should not have any previous change", versionA.getPreviousChange());
		SchemaChange<?> secondLastChange = versionA.getNextChange().getNextChange();
		SchemaChange<?> lastChange = secondLastChange.getNextChange();
		assertNull("This is the last change in the chain and thus no next change should be set", lastChange.getNextChange());
		assertEquals("The previous change from the last change should be the second last change.", secondLastChange.getUuid(),
			lastChange.getPreviousChange().getUuid());

		assertEquals("The last change should be connected to the containerB but it was not.", versionB.getUuid(),
			lastChange.getNextContainerVersion().getUuid());
		assertNull("The change has no from schema container because it it part of a chain of changes.", lastChange.getPreviousContainerVersion());

		assertEquals("The previous change of the schema that was connected to the last change did not match the last change.", lastChange.getUuid(),
			lastChange.getNextContainerVersion().getPreviousChange().getUuid());

		// Link the chain root to another schema container instead.
		FieldSchemaVersionElement versionC = schemaVersionProvider.apply(container);
		SchemaChange<?> firstChange = versionA.getNextChange();
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
		FieldSchemaVersionElement latest = container.getLatestVersion();
		assertNotNull("There should always be a latest version", latest);
		assertEquals("Version B should represent the latest version but it did not", versionB.getUuid(), latest.getUuid());

	}

}
