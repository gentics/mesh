package com.gentics.mesh.cache;

import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true, customOptionChanger = UnitAbsoluteSizeContentCacheTest.class)
public class UnitAbsoluteSizeContentCacheTest extends AbstractAutoOptionContentCacheTest {

	@Override
	protected String unit() {
		return "K";
	}

	@Override
	protected int size() {
		return 1000;
	}
}
