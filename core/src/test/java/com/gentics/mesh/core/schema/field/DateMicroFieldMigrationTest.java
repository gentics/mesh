package com.gentics.mesh.core.schema.field;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.schema.field.AbstractFieldMigrationTest.MicroschemaTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import static com.gentics.mesh.test.TestSize.FULL;

@MicroschemaTest
@MeshTestSetting(testSize = FULL, startServer = false)
public class DateMicroFieldMigrationTest extends DateFieldMigrationTest {

	@Test
	@Ignore("Not applicable for micronodes")
	@Override
	public void testChangeToBinary() throws Exception {
	}

	@Test
	@Ignore("Not applicable for micronodes")
	@Override
	public void testEmptyChangeToBinary() throws Exception {
	}

	@Test
	@Ignore("Not applicable for micronodes")
	@Override
	public void testChangeToMicronode() throws Exception {
	}

	@Test
	@Ignore("Not applicable for micronodes")
	@Override
	public void testEmptyChangeToMicronode() throws Exception {
	}

	@Test
	@Ignore("Not applicable for micronodes")
	@Override
	public void testChangeToMicronodeList() throws Exception {
	}

	@Test
	@Ignore("Not applicable for micronodes")
	@Override
	public void testEmptyChangeToMicronodeList() throws Exception {
	}
}
