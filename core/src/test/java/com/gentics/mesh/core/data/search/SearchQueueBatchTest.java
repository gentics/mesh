package com.gentics.mesh.core.data.search;

import static com.gentics.mesh.test.TestSize.PROJECT;

import org.junit.Test;

import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = false)
public class SearchQueueBatchTest extends AbstractMeshTest {

	@Test
	public void testDependency() {
		EventQueueBatch batch = EventQueueBatch.create();
		System.out.println(batch);
		batch = EventQueueBatch.create();
		System.out.println(batch);
		batch.add(new MeshElementEventModelImpl());
	}
}
