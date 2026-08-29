package com.gentics.mesh.core;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.category.FailingTests;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(testSize = TestSize.PROJECT, startServer = true, optionChanger = MeshCoreOptionChanger.SMALL_EVENT_LOOP_POOL)
@Category(FailingTests.class)
public class ResponsivenessTest extends AbstractMeshTest {

	@Test
	public void testBlockingGets() throws InterruptedException {
		int numThreads = 100;
		CountDownLatch latch = new CountDownLatch(numThreads);
		for (int i = 0; i < numThreads; i++) {
			new Thread(() -> {
				client().me().blockingGet();
				latch.countDown();
			}).start();
		}
		latch.await();
	}
}
