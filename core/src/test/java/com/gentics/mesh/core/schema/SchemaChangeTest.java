package com.gentics.mesh.core.schema;

import org.junit.Test;

import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;

public class SchemaChangeTest extends AbstractBasicDBTest {
	
	
	@Test
	public void test() {
		SchemaContainer container = getSchemaContainer();
		container.getChangesetForPreviousVersion();
		container.getChangesetForNextVersion();
		container.getNextVersion();
		container.getPreviousVersion();
	}

}
