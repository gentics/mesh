package com.gentics.mesh.test.context;

import static com.gentics.mesh.test.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

import com.gentics.mesh.rest.client.MeshResponse;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/**
 * Collection of helper methods which are useful for testing mesh.
 */
public final class MeshTestHelper {

	public static CyclicBarrier prepareBarrier(int nJobs) {
		// Trx.enableDebug();
		CyclicBarrier barrier = new CyclicBarrier(nJobs);
		// Trx.setBarrier(barrier);
		return barrier;
	}

	/**
	 * Wait for all responses and assert that the requests did not fail.
	 * 
	 * @param set
	 * @param barrier
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 * @throws InvocationTargetException
	 * @throws NoSuchMethodException
	 * @throws SecurityException
	 */
	public static void validateCreation(Set<MeshResponse<?>> set, CyclicBarrier barrier)
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
		// Trx.disableDebug();
		if (barrier != null) {
			assertFalse("The barrier should not break. Somehow not all threads reached the barrier point.", barrier.isBroken());
		}

	}

	public static String getSimpleQuery(String field, String text) {
		JsonObject json = new JsonObject(
				"{\"query\":{\"query_string\":{\"query\":\"supersonic\",\"fields\":[\"fields.content^1.0\"],\"type\":\"phrase\"}}}");
		json.getJsonObject("query").getJsonObject("query_string").put("query", text).put("fields", new JsonArray().add(field)).put("type", "phrase");
		return json.encodePrettily();
	}

	public static String getSimpleTermQuery(String key, String value) {
		JsonObject request = new JsonObject("{\"query\":{\"term\":{}}}");
		request.getJsonObject("query").getJsonObject("term").put(key, new JsonObject().put("value", value).put("boost", 1));
		return request.encodePrettily();
	}

	public static String getSimpleWildCardQuery(String key, String value) {
		JsonObject json = new JsonObject("{\"query\":{\"bool\":{\"must\":[{\"wildcard\":{}}]}}}");
		JsonObject wildcard = json.getJsonObject("query").getJsonObject("bool").getJsonArray("must").getJsonObject(0).getJsonObject("wildcard");
		wildcard.put(key, new JsonObject().put("wildcard", value).put("boost", 1));
		return json.encodePrettily();
	}

	public static String getRangeQuery(String fieldName, double from, double to) {
		JsonObject json = new JsonObject();
		json.put("query", new JsonObject().put("range", new JsonObject().put(fieldName,
				new JsonObject().put("from", from).put("to", to).put("include_lower", true).put("include_upper", true).put("boost", 1))));
		return json.encodePrettily();
	}

}
