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

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true, customOptionChanger = NodeMassiveRandomPublishTestNoCache.class)
@Category({FailingTests.class})
//Not actually failing, but may last long
public class NodeMassiveRandomPublishTestNoCache extends AbstractMassivePublishTest implements MeshOptionChanger {

	@Override
	public void change(MeshOptions options) {
		HibernateMeshOptions.class.cast(options).getStorageOptions().setSecondLevelCacheEnabled(false);
	}

	@Override
	protected Optional<BiFunction<UUID, Integer, UUID>> getMaybeUuidProvider() {
		return Optional.empty();
	}
}
