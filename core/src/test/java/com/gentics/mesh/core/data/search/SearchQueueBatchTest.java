package com.gentics.mesh.core.data.search;

import org.junit.Test;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.test.AbstractDBTest;

public class SearchQueueBatchTest extends AbstractDBTest {

	@Test
	public void testDependency() {
		SearchQueueBatch batch = meshDagger.searchQueue().create();
		System.out.println(batch);
		batch = meshDagger.searchQueue().create();
		System.out.println(batch);
		batch.createIndex(null, null, User.class);
	}
}
