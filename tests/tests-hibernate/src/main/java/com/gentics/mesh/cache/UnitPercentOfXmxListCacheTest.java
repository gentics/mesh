package com.gentics.mesh.cache;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.gentics.mesh.cache.CacheStatus;
import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true, customOptionChanger = UnitPercentOfXmxListCacheTest.class)
public class UnitPercentOfXmxListCacheTest extends AbstractAutoOptionListCacheTest {

	@Override
	protected String unit() {
		return "%";
	}

	@Override
	protected int size() {
		return 10;
	}

	@Override
	protected void checkStats(CacheStatus stats) {
		long maxJvmMemory = Runtime.getRuntime().maxMemory();
		long allowedJvmMemory = maxJvmMemory / 100 * size();
		assertEquals(size() + unit(), stats.getSetup());
		assertEquals(allowedJvmMemory, stats.getMaxSizeInUnits());
		assertTrue(allowedJvmMemory >= stats.getCurrentSizeInUnits());
	}
}
