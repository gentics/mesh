package com.gentics.mesh.core.schema;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainerVersion;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.RemoveFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class SchemaChangeTest extends AbstractMeshTest{

	@Test
	public void testDomainModel() {
		try (Tx tx = tx()) {
			SchemaContainer container = tx.getGraph().addFramedVertex(SchemaContainerImpl.class);

			SchemaContainerVersion versionA = tx.getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
			SchemaContainerVersion versionB = tx.getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
			SchemaContainerVersion versionC = tx.getGraph().addFramedVertex(SchemaContainerVersionImpl.class);

			RemoveFieldChange change = tx.getGraph().addFramedVertex(RemoveFieldChangeImpl.class);
			assertNull("Initially no version should have been set", container.getLatestVersion());
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
			MicroschemaContainer container = tx.getGraph().addFramedVertex(MicroschemaContainerImpl.class);

			MicroschemaContainerVersion versionA = tx.getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			MicroschemaContainerVersion versionB = tx.getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			container.setLatestVersion(versionB);
			SchemaChange<?> oldChange = chainChanges(versionA, versionB);
			validate(container, versionA, versionB, oldChange);
		}
	}

	@Test
	public void testChangeChain() {
		try (Tx tx = tx()) {
			SchemaContainer container = tx.getGraph().addFramedVertex(SchemaContainerImpl.class);
			SchemaContainerVersion versionA = tx.getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
			SchemaContainerVersion versionB = tx.getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
			container.setLatestVersion(versionA);
			SchemaChange<?> oldChange = chainChanges(versionA, versionB);
			validate(container, versionA, versionB, oldChange);
		}
	}

	/**
	 * Chain multiple changes in between the given versions.
	 * 
	 * @param versionA
	 * @param versionB
	 * @return
	 */
	private SchemaChange<?> chainChanges(GraphFieldSchemaContainerVersion versionA, GraphFieldSchemaContainerVersion versionB) {
		SchemaChange<?> oldChange = null;
		for (int i = 0; i < 3; i++) {
			SchemaChange<?> change = Tx.getActive().getGraph().addFramedVertex(RemoveFieldChangeImpl.class);
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
	private void validate(GraphFieldSchemaContainer container, GraphFieldSchemaContainerVersion versionA, GraphFieldSchemaContainerVersion versionB,
			SchemaChange<?> oldChange) {
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
		SchemaContainerVersion versionC = Tx.getActive().getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
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
		GraphFieldSchemaContainerVersion latest = container.getLatestVersion();
		assertNotNull("There should always be a latest version", latest);
		assertEquals("Version B should represent the latest version but it did not", versionB.getUuid(), latest.getUuid());

	}

}
