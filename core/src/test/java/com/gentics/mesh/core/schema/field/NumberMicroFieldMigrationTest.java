package com.gentics.mesh.core.schema.field;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.schema.field.AbstractFieldMigrationTest.MicroschemaTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MicroschemaTest
@MeshTestSetting(useElasticsearch = false, useTinyDataset = false, startServer = false)
public class NumberMicroFieldMigrationTest extends NumberFieldMigrationTest {

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
