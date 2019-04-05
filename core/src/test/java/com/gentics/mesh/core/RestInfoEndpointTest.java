package com.gentics.mesh.core;

import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.call;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.dagger.DB;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.orientechnologies.orient.core.OConstants;

import io.vertx.core.impl.launcher.commands.VersionCommand;


@MeshTestSetting(testSize = FULL, startServer = true)
public class RestInfoEndpointTest extends AbstractMeshTest {

	@Test
	public void testGetInfo() {
		MeshServerInfoModel info = call(() -> client().getApiInfo());
		assertEquals(Mesh.getPlainVersion(), info.getMeshVersion());
		assertEquals("orientdb", info.getDatabaseVendor());
		assertEquals("dev-null", info.getSearchVendor());
		assertEquals(VersionCommand.getVersion(), info.getVertxVersion());
		assertEquals(Mesh.mesh().getOptions().getNodeName(), info.getMeshNodeName());
		assertEquals("The database version did not match.", OConstants.getVersion(), info.getDatabaseVersion());
		assertEquals("1.0", info.getSearchVersion());
		assertEquals(DB.get().getDatabaseRevision(), info.getDatabaseRevision());
	}

	@Test
	public void testLoadRAML() {
		String raml = call(() -> client().getRAML());
		assertNotNull(raml);
	}
}
