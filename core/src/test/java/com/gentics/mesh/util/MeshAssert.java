package com.gentics.mesh.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.etc.MeshSpringConfiguration;

public final class MeshAssert {

	public static void failingLatch(CountDownLatch latch) throws InterruptedException {
		if (!latch.await(1, TimeUnit.SECONDS)) {
			fail("Latch timeout reached");
		}
	}

	public static void assertDeleted(Map<String, String> uuidToBeDeleted) {
		for (Map.Entry<String, String> entry : uuidToBeDeleted.entrySet()) {
			assertFalse("One vertex was not deleted. Uuid: {" + entry.getValue() + "} - Type: {" + entry.getKey() + "}", MeshSpringConfiguration
					.getMeshSpringConfiguration().framedThreadedTransactionalGraph().v().has("uuid", entry.getValue()).hasNext());
		}
	}

}
