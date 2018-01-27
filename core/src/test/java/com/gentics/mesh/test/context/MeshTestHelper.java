package com.gentics.mesh.test.context;

import static com.gentics.mesh.test.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
import static org.junit.Assert.assertFalse;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.gentics.mesh.rest.client.MeshResponse;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Collection of helper methods which are useful for testing mesh.
 */
public final class MeshTestHelper {

	private static final Logger log = LoggerFactory.getLogger(MeshTestHelper.class);

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

	public static String getSimpleQuery(String field, String text) throws JSONException {
		QueryStringQueryBuilder qb = QueryBuilders.queryStringQuery(text);
		qb.type(Type.PHRASE);
		qb.field(field);

		JSONObject request = new JSONObject();
		request.put("query", new JSONObject(qb.toString()));
		String query = request.toString();
		if (log.isDebugEnabled()) {
			log.debug(query);
		}
		return query;
	}

	public static String getSimpleTermQuery(String key, String value) throws JSONException {
		QueryBuilder qb = QueryBuilders.termQuery(key, value);

		JSONObject request = new JSONObject();
		request.put("query", new JSONObject(qb.toString()));
		String query = request.toString();
		if (log.isDebugEnabled()) {
			log.debug(query);
		}
		return query;
	}

	public static String getSimpleWildCardQuery(String key, String value) throws JSONException {
		QueryBuilder qb = QueryBuilders.wildcardQuery(key, value);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(qb);

		JSONObject request = new JSONObject();
		request.put("query", new JSONObject(bqb.toString()));
		String query = request.toString();
		if (log.isDebugEnabled()) {
			log.debug(query);
		}
		return query;
	}

	public static String getRangeQuery(String fieldName, double from, double to) throws JSONException {
		RangeQueryBuilder range = QueryBuilders.rangeQuery(fieldName).from(from).to(to);
		return "{ \"query\": " + range.toString() + "}";
	}

}
