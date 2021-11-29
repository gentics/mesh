package com.gentics.mesh.core;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.orientechnologies.orient.core.OConstants;

@MeshTestSetting(testSize = FULL, startServer = true)
public class OrientDBRestInfoEndpointTest extends AbstractMeshTest {

	@Test
	public void testInfo() {
		MeshServerInfoModel info = call(() -> client().getApiInfo());
		assertEquals("The database version did not match.", OConstants.getVersion(), info.getDatabaseVersion());
		assertEquals(db().getDatabaseRevision(), info.getDatabaseRevision());
	}

	@Test
	public void testMonitoringInfo() {
		MeshServerInfoModel info = call(() -> monClient().versions());
		assertEquals("The database version did not match.", OConstants.getVersion(), info.getDatabaseVersion());
		assertEquals(db().getDatabaseRevision(), info.getDatabaseRevision());
	}
}
