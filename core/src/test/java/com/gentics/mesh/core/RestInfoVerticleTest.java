package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.MeshNameProvider;
import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.core.verticle.admin.RestInfoVerticle;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

public class RestInfoVerticleTest extends AbstractIsolatedRestVerticleTest {

	private RestInfoVerticle infoVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(infoVerticle);
		return list;
	}

	@Test
	public void testGetInfo() {
		MeshServerInfoModel info = call(() -> getClient().getApiInfo());
		assertEquals(Mesh.getPlainVersion(), info.getMeshVersion());
		assertEquals("orientdb", info.getDatabaseVendor());
		assertEquals("dummy", info.getSearchVendor());
		assertEquals(new io.vertx.core.Starter().getVersion(), info.getVertxVersion());
		assertEquals(MeshNameProvider.getInstance().getName(), info.getMeshNodeId());
		assertEquals("The database version did not match.", "2.2.x", info.getDatabaseVersion());
		assertEquals("1.0", info.getSearchVersion());
	}

}
