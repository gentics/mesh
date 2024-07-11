package com.gentics.mesh.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;

import com.gentics.mesh.cache.CacheStatusModel;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.ConfigUtils;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshOptionChanger;

public abstract class AbstractAutoOptionContentCacheTest extends AbstractContentCacheTest implements MeshOptionChanger {

	@Override
	public void change(MeshOptions options) {
		((HibernateMeshOptions) options).getCacheConfig().setFieldContainerCacheSize(size() + unit());
	}

	@Override
	protected void checkStats(CacheStatusModel stats) {
		String rawSize = size()  + unit();
		long sizeAvg = parseRawSize(rawSize);
		assertEquals(((HibernateMeshOptions) options()).getCacheConfig().getFieldContainerCacheSize(), stats.getSetup());
		assertEquals(sizeAvg, stats.getMaxSizeInUnits());
		assertTrue(sizeAvg >= stats.getCurrentSizeInUnits());
		assertTrue(0 < stats.getCurrentSizeInUnits());
	}

	protected long parseRawSize(String cacheSizeRaw) {
		Matcher percentageMatcher = ConfigUtils.QUOTA_PATTERN_PERCENTAGE.matcher(cacheSizeRaw);
		Matcher sizeMatcher = ConfigUtils.QUOTA_PATTERN_SIZE.matcher(cacheSizeRaw);
		Matcher numberMatcher = ConfigUtils.QUOTA_PATTERN_NUMBER.matcher(cacheSizeRaw);
		if (percentageMatcher.matches()) {
			return Runtime.getRuntime().maxMemory() / 100L * Long.parseLong(percentageMatcher.group("value"));
		} else if (sizeMatcher.matches()) {
			return ConfigUtils.getBytes(sizeMatcher);
		} else if (numberMatcher.matches()) {
			return Long.parseLong(numberMatcher.group("value"));
		} else {
			return 0;
		}
	}

	protected abstract String unit();

	protected abstract int size();
}
