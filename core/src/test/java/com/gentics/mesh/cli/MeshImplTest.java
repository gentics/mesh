package com.gentics.mesh.cli;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.impl.MeshFactoryImpl;

public class MeshImplTest {

	@Test
	public void testHostname() throws Exception {
		MeshImpl mesh = new MeshImpl(new MeshOptions());
		assertNotNull(mesh.getHostname());
	}

	@Test
	public void testUpdateCheck() throws Exception {
		Mesh mesh = new MeshFactoryImpl().create();
		((MeshImpl) mesh).invokeUpdateCheck();
	}

}
