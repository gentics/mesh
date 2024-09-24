package com.gentics.mesh.cache;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.cache.CacheStatus;
import com.gentics.mesh.contentoperation.ContentCachedStorage;
import com.gentics.mesh.core.node.AbstractMassiveNodeLoadTest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.dagger.HibernateMeshComponent;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;

public abstract class AbstractContentCacheTest extends AbstractMassiveNodeLoadTest {

	public CacheStatus getCacheStats() {
		HibernateMeshComponent mesh = ((HibernateMeshComponent)mesh());
		ContentCachedStorage storage = mesh.contentCacheStorage();
		return storage.getStatus();
	}

	@Test
	public void testReadAll() {
		NodeListResponse nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, parentFolderUuid, new VersioningParametersImpl().draft()));
		assertEquals("The subnode did not contain the created node", numOfNodesPerLevel, nodeList.getData().size());

		CacheStatus stats = getCacheStats();
		checkStats(stats);
	}

	protected abstract void checkStats(CacheStatus stats);
}
