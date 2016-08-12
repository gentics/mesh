package com.gentics.mesh.test;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.definition.CrudVerticleTestCases;
import com.gentics.mesh.test.definition.MultithreadingTestCases;

public abstract class AbstractBasicIsolatedCrudVerticleTest extends AbstractIsolatedRestVerticleTest implements MultithreadingTestCases, CrudVerticleTestCases {

	protected void validateDeletion(Set<MeshResponse<GenericMessageResponse>> set, CyclicBarrier barrier) {
		boolean foundDelete = false;
		for (MeshResponse<GenericMessageResponse> future : set) {
			latchFor(future);
			if (future.succeeded() && future.result() != null) {
				foundDelete = true;
				continue;
			}
			if (future.succeeded() && future.result() != null && foundDelete == true) {
				fail("We found more than one request that succeeded. Only one of the requests should be able to delete the node.");
			}
		}
		assertTrue(foundDelete);

		//		Trx.disableDebug();
		if (barrier != null) {
			assertFalse("The barrier should not break. Somehow not all threads reached the barrier point.", barrier.isBroken());
		}
	}

	protected void validateSet(Set<MeshResponse<?>> set, CyclicBarrier barrier) {
		for (MeshResponse<?> future : set) {
			latchFor(future);
			assertSuccess(future);
		}
		//		Trx.disableDebug();
		if (barrier != null) {
			assertFalse("The barrier should not break. Somehow not all threads reached the barrier point.", barrier.isBroken());
		}
	}

	protected void validateFutures(Set<MeshResponse<?>> set) {
		for (MeshResponse<?> future : set) {
			latchFor(future);
			assertSuccess(future);
		}
	}

	protected void validateCreation(Set<MeshResponse<?>> set, CyclicBarrier barrier)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Set<String> uuids = new HashSet<>();
		for (MeshResponse<?> future : set) {
			latchFor(future);
			assertSuccess(future);
			Object result = future.result();
			// Rest responses do not share a common class. We just use reflection to extract the uuid from the response pojo
			Object uuidObject = result.getClass().getMethod("getUuid").invoke(result);
			String currentUuid = uuidObject.toString();
			assertFalse("The rest api returned a response with a uuid that was returned before. Each create request must always be atomic.",
					uuids.contains(currentUuid));
			uuids.add(currentUuid);
		}
		//		Trx.disableDebug();
		if (barrier != null) {
			assertFalse("The barrier should not break. Somehow not all threads reached the barrier point.", barrier.isBroken());
		}

	}

	protected CyclicBarrier prepareBarrier(int nJobs) {
		//		Trx.enableDebug();
		CyclicBarrier barrier = new CyclicBarrier(nJobs);
		//		Trx.setBarrier(barrier);
		return barrier;
	}
}
