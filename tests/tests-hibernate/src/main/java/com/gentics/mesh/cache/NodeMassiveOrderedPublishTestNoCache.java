package com.gentics.mesh.cache;

import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestSize.FULL;

import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

import org.junit.experimental.categories.Category;

import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.category.FailingTests;
import com.gentics.mesh.util.UUIDUtil;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true, customOptionChanger = NodeMassiveOrderedPublishTestNoCache.class)
@Category({FailingTests.class})
//Not actually failing, but may last long
public class NodeMassiveOrderedPublishTestNoCache extends AbstractMassivePublishTest implements MeshOptionChanger {

	@Override
	public void change(MeshOptions options) {
		HibernateMeshOptions.class.cast(options).getStorageOptions().setSecondLevelCacheEnabled(false);
	}

	@Override
	protected Optional<BiFunction<UUID, Integer, UUID>> getMaybeUuidProvider() {
		return Optional.of((uuid, i) -> {
			if (i < 0) {
				i = 0;
				uuid = VERY_BASE_UUID;
			}
			long number = Long.parseLong(UUIDUtil.toShortUuid(uuid), 16);
			// Plus!
			number += i;
			return UUIDUtil.toJavaUuid(Long.toHexString(number));
		});
	}
}
