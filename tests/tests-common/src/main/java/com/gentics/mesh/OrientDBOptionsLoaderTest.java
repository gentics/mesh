package com.gentics.mesh;

import org.junit.Test;

import com.gentics.mesh.etc.config.OrientDBMeshOptions;

public class OrientDBOptionsLoaderTest extends OptionsLoaderTest<OrientDBMeshOptions> {

	@Override
	public OrientDBMeshOptions getOptions() {
		return new OrientDBMeshOptions();
	}

	@Test(expected = NullPointerException.class)
	public void testInvalidOptions4() {
		OrientDBMeshOptions options = getOptions();
		options.getStorageOptions().setDirectory(null);
		options.getStorageOptions().setStartServer(true);
		options.validate();
	}

	@Test
	public void testInvalidOptions5() {
		OrientDBMeshOptions options = getOptions();
		options.setNodeName("ABC");
		options.getAuthenticationOptions().setKeystorePassword("ABC");
		options.getStorageOptions().setDirectory(null);
		options.validate();
	}
}
