package com.gentics.mesh.core.schema.field;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.schema.field.AbstractFieldMigrationTest.MicroschemaTest;

@MicroschemaTest
public class HtmlListMicroFieldMigrationTest extends HtmlListFieldMigrationTest {
	@Override
	@Ignore("Not applicable for micronodes")
	@Test
	public void testChangeToBinary() throws Exception {
	}

	@Override
	@Ignore("Not applicable for micronodes")
	@Test
	public void testChangeToMicronode() throws Exception {
	}

	@Override
	@Ignore("Not applicable for micronodes")
	@Test
	public void testChangeToMicronodeList() throws Exception {
	}
}
