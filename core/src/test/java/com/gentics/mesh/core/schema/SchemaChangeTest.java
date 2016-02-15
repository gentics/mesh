package com.gentics.mesh.core.schema;

import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.schema.GraphFieldSchemaContainer;
import com.gentics.mesh.core.data.schema.RemoveFieldChange;
import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.impl.RemoveFieldChangeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.graphdb.spi.Database;

public class SchemaChangeTest extends AbstractBasicDBTest {

	@Test
	public void testDomainModel() {

		SchemaContainer containerA = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		SchemaContainer containerB = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		SchemaContainer containerC = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);

		RemoveFieldChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
		assertNull("The previous change should be null since we did not link it to any schema container.", containerA.getPreviousChange());
		assertNull("The next change should be null since we did not link it to any schema container.", containerA.getNextChange());

		containerA.setNextChange(change);
		assertNotNull("The next change was not found but we linked it to the schema container.", containerA.getNextChange());

		containerA.setPreviousChange(change);
		assertNotNull("The previous change was not found but we linked it to the schema container.", containerA.getPreviousChange());

		assertNull("The next version was not yet set and thus should be null but it was not.", containerB.getNextVersion());
		containerB.setNextVersion(containerC);
		assertNotNull(containerB.getNextVersion());

		assertNull("The next version was not yet set and thus should be null but it was not.", containerB.getPreviousVersion());
		containerB.setPreviousVersion(containerA);
		assertNotNull(containerB.getPreviousVersion());

	}

	@Test
	public void testMicroschemaChanges() {
		MicroschemaContainer containerA = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerImpl.class);
		MicroschemaContainer containerB = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerImpl.class);
		SchemaChange oldChange = chainChanges(containerA, containerB);
		validate(containerA, containerB, oldChange);
	}

	@Test
	public void testChangeChain() {
		SchemaContainer containerA = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		SchemaContainer containerB = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		SchemaChange oldChange = chainChanges(containerA, containerB);
		validate(containerA, containerB, oldChange);
	}

	private SchemaChange chainChanges(GraphFieldSchemaContainer containerA, GraphFieldSchemaContainer containerB) {
		SchemaChange oldChange = null;
		for (int i = 0; i < 3; i++) {
			SchemaChange change = Database.getThreadLocalGraph().addFramedVertex(RemoveFieldChangeImpl.class);
			if (oldChange == null) {
				oldChange = change;
				assertNull("The change has not yet been connected to any schema", oldChange.getPreviousContainer());
				containerA.setNextChange(oldChange);
				assertNotNull("The change has been connected to the schema container and thus the connection should be loadable",
						oldChange.getPreviousContainer());
			} else {
				oldChange.setNextChange(change);
				oldChange = change;
			}
		}
		return oldChange;
	}

	/**
	 * Validate the chain of changes in between the containers.
	 * @param containerA
	 * @param containerB
	 * @param oldChange
	 */
	private void validate(GraphFieldSchemaContainer containerA, GraphFieldSchemaContainer containerB, SchemaChange oldChange) {
		containerA.setNextVersion(containerB);
		assertNull(oldChange.getNextContainer());
		oldChange.setNextSchemaContainer(containerB);
		assertNotNull(oldChange.getNextContainer());
		assertNotNull("The containerA should have a next change", containerA.getNextChange());
		assertNull("The container should not have any previous change", containerA.getPreviousChange());
		SchemaChange secondLastChange = containerA.getNextChange().getNextChange();
		SchemaChange lastChange = secondLastChange.getNextChange();
		assertNull("This is the last change in the chain and thus no next change should be set", lastChange.getNextChange());
		assertEquals("The previous change from the last change should be the second last change.", secondLastChange.getUuid(),
				lastChange.getPreviousChange().getUuid());

		assertEquals("The last change should be connected to the containerB but it was not.", containerB.getUuid(),
				lastChange.getNextContainer().getUuid());
		assertNull("The change has no from schema container because it it part of a chain of changes.", lastChange.getPreviousContainer());

		assertEquals("The previous change of the schema that was connected to the last change did not match the last change.", lastChange.getUuid(),
				lastChange.getNextContainer().getPreviousChange().getUuid());

		// Link the chain root to another schema container instead.
		SchemaContainer containerC = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		SchemaChange firstChange = containerA.getNextChange();
		firstChange.setPreviousContainer(containerC);
		assertNotEquals("The first change should no longer be connected to containerA", containerA.getUuid(),
				firstChange.getPreviousContainer().getUuid());
		assertEquals("The chain of changes should now be connected to containerC", containerC.getUuid(),
				firstChange.getPreviousContainer().getUuid());

		// Check next version
		assertNotNull("Container A should have a next version.", containerA.getNextVersion());
		assertEquals("Container B should be the next version of container A.", containerB.getUuid(), containerA.getNextVersion().getUuid());

		// Check latest version
		GraphFieldSchemaContainer latest = containerA.getLatestVersion();
		assertNotNull("There is always a latest version", latest);
		assertEquals("Container B represents the latest version.", containerB.getUuid(), latest.getUuid());

	}

}
