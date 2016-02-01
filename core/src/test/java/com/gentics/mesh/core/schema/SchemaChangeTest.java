package com.gentics.mesh.core.schema;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.schema.SchemaChange;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.impl.SchemaChangeImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.graphdb.spi.Database;

public class SchemaChangeTest extends AbstractBasicDBTest {

	@Test
	public void testDomainModel() {

		SchemaContainer containerA = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		SchemaContainer containerB = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		SchemaContainer containerC = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);

		SchemaChange change = Database.getThreadLocalGraph().addFramedVertex(SchemaChangeImpl.class);
		assertNull("The previous change should be null since we did not link it to any schema container.", containerA.getPreviousChange());
		assertNull("The next change should be null since we did not link it to any schema container.", containerA.getNextChange());

		containerA.setNextChange(change);
		assertNotNull("The next change was not found but we linked it to the schema container.", containerA.getNextChange());

		containerA.setPreviousChange(change);
		assertNotNull("The previous change was not found but we linked it to the schema container.", containerA.getPreviousChange());

		assertNull("The next version was not yet set and thus should be null but it was not.", containerB.getNextVersion());
		containerB.setNextVersion(containerC);
		assertNotNull(containerB.getNextVersion());

		assertNull("The next version was not yet set and thus should be null but it was not.", containerB.getNextVersion());
		containerB.setPreviousVersion(containerA);
		assertNotNull(containerB.getPreviousVersion());

	}

}
