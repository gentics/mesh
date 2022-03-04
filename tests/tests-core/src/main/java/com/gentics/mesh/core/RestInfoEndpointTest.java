package com.gentics.mesh.core;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.impl.launcher.commands.VersionCommand;

@MeshTestSetting(testSize = FULL, startServer = true)
public class RestInfoEndpointTest extends AbstractMeshTest {

	@Test
	public void testGetInfoWithDisabledServerTokens() {
		options().getHttpServerOptions().setServerTokens(false);
		MeshServerInfoModel info = call(() -> client().getApiInfo());
		assertNull(info.getMeshVersion());

		grantAdmin();
		info = call(() -> client().getApiInfo());
		assertNotNull(info.getMeshVersion());
	}

	@Test
	public void testGetInfo() {
		MeshServerInfoModel info = call(() -> client().getApiInfo());
		assertEquals(Mesh.getPlainVersion(), info.getMeshVersion());
		assertEquals("orientdb", info.getDatabaseVendor());
		assertEquals("dev-null", info.getSearchVendor());
		assertEquals(VersionCommand.getVersion(), info.getVertxVersion());
		assertEquals(options().getNodeName(), info.getMeshNodeName());
		assertEquals("1.0", info.getSearchVersion());
		assertEquals(db().getDatabaseRevision(), info.getDatabaseRevision());
	}

	@Test
	public void testLoadRAML() {
		grantAdmin();
		String raml = call(() -> client().getRAML());
		assertNotNull(raml);
	}
}
