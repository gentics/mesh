package com.gentics.mesh.cli;

import static org.junit.Assert.*;

import org.junit.Test;

import com.gentics.mesh.etc.config.MeshOptions;

public class MeshImplTest {

	@Test
	public void testHostname() throws Exception {
		MeshImpl mesh = new MeshImpl(new MeshOptions());
		assertNotNull(mesh.getHostname());
	}

	@Test
	public void testUpdateCheck() throws Exception {
		MeshImpl mesh = new MeshImpl(new MeshOptions());
		mesh.invokeUpdateCheck();
	}

}
