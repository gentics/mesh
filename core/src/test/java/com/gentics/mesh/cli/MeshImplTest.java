package com.gentics.mesh.cli;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.impl.MeshFactoryImpl;

import io.vertx.core.Vertx;

public class MeshImplTest {

	@Test
	public void testHostname() throws Exception {
		MeshImpl mesh = new MeshImpl(new MeshOptions());
		assertNotNull(mesh.getHostname());
	}

	@Test
	public void testUpdateCheck() throws Exception {
		Mesh mesh = new MeshFactoryImpl().mesh();
		// This would normally be set during init of mesh
		((MeshImpl) mesh).setVertx(Vertx.vertx());
		assertNotNull("Update check failed", ((MeshImpl) mesh).invokeUpdateCheck());
	}

}
