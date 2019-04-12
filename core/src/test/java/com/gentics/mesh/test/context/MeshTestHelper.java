package com.gentics.mesh.test.context;

import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import io.reactivex.Observable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.Assert.assertFalse;

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
	 * @param uuidList
	 */
	public static void validateCreation(List<String> uuidList) {
		Set<String> uuids = new HashSet<>();
		for (String currentUuid : uuidList) {
			assertFalse("The rest api returned a response with a uuid that was returned before. Each create request must always be atomic.",
					uuids.contains(currentUuid));
			uuids.add(currentUuid);
		}
	}

	public static void validateCreation(int count, Function<Integer, MeshRequest<? extends AbstractResponse>> creator) {
		List<String> uuids = Observable.range(0, count)
			.flatMapSingle(i -> creator.apply(i).toSingle())
			.map(AbstractResponse::getUuid)
			.toList()
			.blockingGet();
		validateCreation(uuids);
	}

	public static void awaitConcurrentRequests(int count, Function<Integer, MeshRequest<?>> creator) {
		Observable.range(0, count)
			.flatMapCompletable(i -> creator.apply(i).toCompletable())
			.blockingAwait();
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

	public static <T> Consumer<T> noopConsumer() {
		return t -> {};
	}
}
