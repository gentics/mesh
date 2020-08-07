package com.gentics.mesh.core.binary;

import org.junit.Test;

import com.gentics.mesh.plugin.AbstractPluginTest;
import com.gentics.mesh.plugin.BinaryStorageTestPlugin;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class BinaryStoragePluginTest extends AbstractPluginTest {

	@Test
	public void testBinaryPlugin() {
		grantAdminRole();

		for (int i = 1; i <= 100; i++) {
			deployPlugin(BinaryStorageTestPlugin.class, "bin" + i);
		}

		waitForPluginRegistration();
		// TODO upload and assert
		// TODO download and assert
		// TODO range request and assert
		// TODO delete and assert
	}
}
