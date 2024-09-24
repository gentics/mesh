package com.gentics.mesh.cache;

import static com.gentics.mesh.test.TestSize.FULL;

import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true, customOptionChanger = UnitNumberListCacheTest.class)
public class UnitNumberListCacheTest extends AbstractAutoOptionListCacheTest {

	@Override
	protected String unit() {
		return "";
	}

	@Override
	protected int size() {
		return 100;
	}
}
