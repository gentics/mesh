package com.gentics.mesh.core.schema.field;

import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.schema.field.AbstractFieldMigrationTest.MicroschemaTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MicroschemaTest
@MeshTestSetting(testSize = FULL, startServer = false)
public class BooleanMicroFieldMigrationTest extends BooleanFieldMigrationTest {

	@Test
	@Override
	@Ignore("Not applicable for micronodes")
	public void testChangeToBinary() throws Exception {
	}

	@Test
	@Override
	@Ignore("Not applicable for micronodes")
	public void testEmptyChangeToBinary() throws Exception {
	}

	@Test
	@Override
	@Ignore("Not applicable for micronodes")
	public void testChangeToMicronode() throws Exception {
	}

	@Test
	@Override
	@Ignore("Not applicable for micronodes")
	public void testEmptyChangeToMicronode() throws Exception {
	}

	@Test
	@Override
	@Ignore("Not applicable for micronodes")
	public void testChangeToMicronodeList() throws Exception {
	}

	@Test
	@Override
	@Ignore("Not applicable for micronodes")
	public void testEmptyChangeToMicronodeList() throws Exception {
	}
}
