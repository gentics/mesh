package com.gentics.mesh.rest;

import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true, customOptionChanger = RestModelMinifierTest.class)
public class RestModelMinifierTest extends RestModelDefaultMinifierTest implements MeshOptionChanger {

	@Override
	public void change(MeshOptions options) {
		options.getHttpServerOptions().setMinifyJson(true);
	}
}
