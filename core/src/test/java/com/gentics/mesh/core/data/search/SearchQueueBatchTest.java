package com.gentics.mesh.core.data.search;

import static com.gentics.mesh.test.TestSize.PROJECT;

import org.junit.Test;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.event.impl.EventQueueBatchImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = false)
public class SearchQueueBatchTest extends AbstractMeshTest {

	@Test
	public void testDependency() {
		EventQueueBatch batch = new EventQueueBatchImpl();
		System.out.println(batch);
		batch = new EventQueueBatchImpl();
		System.out.println(batch);
		batch.createIndex(null, User.class);
	}
}
