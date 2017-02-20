package com.gentics.mesh.core.schema.field;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.schema.field.AbstractFieldMigrationTest.MicroschemaTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MicroschemaTest
@MeshTestSetting(useElasticsearch = false, useTinyDataset = false, startServer = false)
public class DateListMicroFieldMigrationTest extends DateListFieldMigrationTest {

	@Test
	@Override
	@Ignore("Not applicable for micronodes")
	public void testChangeToBinary() throws Exception {
	}

	@Test
	@Override
	@Ignore("Not applicable for micronodes")
	public void testChangeToMicronode() throws Exception {
	}

	@Test
	@Override
	@Ignore("Not applicable for micronodes")
	public void testChangeToMicronodeList() throws Exception {
	}
}
