package com.gentics.mesh;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import com.gentics.mesh.etc.config.MeshOptions;

public class OptionsLoaderTest {

	@Test
	public void testOptionsLoader() {
		File confFile = new File(OptionsLoader.MESH_CONF_FILENAME);
		if (confFile.exists()) {
			confFile.delete();
		}
		MeshOptions options = OptionsLoader.createOrloadOptions();
		assertNotNull(options);
		assertTrue("The file should have been created.", confFile.exists());
	}
}
