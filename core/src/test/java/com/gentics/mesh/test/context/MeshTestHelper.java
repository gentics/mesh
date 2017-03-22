package com.gentics.mesh.test.context;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;

import com.gentics.mesh.core.data.service.I18NUtil;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClientJsonObjectException;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Collection of helper methods which are useful for testing mesh.
 */
public final class MeshTestHelper {

	private static final Logger log = LoggerFactory.getLogger(MeshTestHelper.class);

	public static CyclicBarrier prepareBarrier(int nJobs) {
		//		Trx.enableDebug();
		CyclicBarrier barrier = new CyclicBarrier(nJobs);
		//		Trx.setBarrier(barrier);
		return barrier;
	}

	public static void assertEqualsSanitizedJson(String msg, String expectedJson, String unsanitizedResponseJson) {
		String sanitizedJson = unsanitizedResponseJson.replaceAll("uuid\":\"[^\"]*\"", "uuid\":\"uuid-value\"");
		assertEquals(msg, expectedJson, sanitizedJson);
	}

	public static void expectResponseMessage(GenericMessageResponse response, String i18nKey, String... i18nParams) {
		Locale en = Locale.ENGLISH;
		String message = I18NUtil.get(en, i18nKey, i18nParams);
		assertEquals("The response message does not match.", message, response.getMessage());
	}

	public static void expectFailureMessage(MeshResponse<?> future, HttpResponseStatus status, String message) {
		assertTrue("We expected the future to have failed but it succeeded.", future.failed());
		assertNotNull(future.cause());

		if (future.cause() instanceof MeshRestClientMessageException) {
			MeshRestClientMessageException exception = ((MeshRestClientMessageException) future.cause());
			assertEquals("The status code of the nested exception did not match the expected value.", status.code(), exception.getStatusCode());
			assertEquals(message, exception.getMessage());
		} else {
			future.cause()
					.printStackTrace();
			fail("Unhandled exception");
		}
	}

	public static void expectException(MeshResponse<?> future, HttpResponseStatus status, String bodyMessageI18nKey, String... i18nParams) {
		Locale en = Locale.ENGLISH;
		String message = I18NUtil.get(en, bodyMessageI18nKey, i18nParams);
		assertNotEquals("Translation for key " + bodyMessageI18nKey + " not found", message, bodyMessageI18nKey);
		expectFailureMessage(future, status, message);
	}

	/**
	 * Call the given handler, latch for the future and assert success. Then return the result.
	 * 
	 * @param handler
	 *            handler
	 * @param <T>
	 *            type of the returned object
	 * @return result of the future
	 */
	public static <T> T call(ClientHandler<T> handler) {
		MeshResponse<T> future;
		try {
			future = handler.handle()
					.invoke();
		} catch (Exception e) {
			future = new MeshResponse<>(Future.failedFuture(e));
		}
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	/**
	 * Returns the failure response and asserts the given status code.
	 * 
	 * @param handler
	 * @param status
	 * @return
	 * @throws Throwable 
	 */
	public static JsonObject call(ClientHandler<JsonObject> handler, HttpResponseStatus status) throws Throwable {
		MeshResponse<JsonObject> future;
		try {
			future = handler.handle()
					.invoke();
		} catch (Exception e) {
			future = new MeshResponse<>(Future.failedFuture(e));
		}
		latchFor(future);
		if (!future.failed()) {
			fail("Request should have failed but it did not.");
		}
		if (future.cause() instanceof MeshRestClientJsonObjectException) {
			MeshRestClientJsonObjectException error = (MeshRestClientJsonObjectException) future.cause();
			return error.getResponseInfo();
		}
		throw future.cause();
	}

	/**
	 * Call the given handler, latch for the future and expect the given failure in the future.
	 *
	 * @param handler
	 *            handler
	 * @param status
	 *            expected response status
	 * @param bodyMessageI18nKey
	 *            i18n of the expected response message
	 * @param i18nParams
	 *            parameters of the expected response message
	 */
	public static <T> void call(ClientHandler<T> handler, HttpResponseStatus status, String bodyMessageI18nKey, String... i18nParams) {
		MeshResponse<T> future;
		try {
			future = handler.handle()
					.invoke();
		} catch (Exception e) {
			future = new MeshResponse<>(Future.failedFuture(e));
		}
		latchFor(future);
		expectException(future, status, bodyMessageI18nKey, i18nParams);
	}

	public static void validateDeletion(Set<MeshResponse<Void>> set, CyclicBarrier barrier) {
		boolean foundDelete = false;
		for (MeshResponse<Void> future : set) {
			latchFor(future);
			if (future.succeeded() && foundDelete == true) {
				fail("We found more than one request that succeeded. Only one of the requests should be able to delete the node.");
			}
			if (future.succeeded()) {
				foundDelete = true;
				continue;
			}
		}
		assertTrue("We did not find a single request which succeeded.", foundDelete);

		//		Trx.disableDebug();
		if (barrier != null) {
			assertFalse("The barrier should not break. Somehow not all threads reached the barrier point.", barrier.isBroken());
		}
	}

	public static void validateSet(Set<MeshResponse<?>> set, CyclicBarrier barrier) {
		for (MeshResponse<?> future : set) {
			latchFor(future);
			assertSuccess(future);
		}
		//		Trx.disableDebug();
		if (barrier != null) {
			assertFalse("The barrier should not break. Somehow not all threads reached the barrier point.", barrier.isBroken());
		}
	}

	public static void validateFutures(Set<MeshResponse<?>> set) {
		for (MeshResponse<?> future : set) {
			latchFor(future);
			assertSuccess(future);
		}
	}

	public static void validateCreation(Set<MeshResponse<?>> set, CyclicBarrier barrier)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		Set<String> uuids = new HashSet<>();
		for (MeshResponse<?> future : set) {
			latchFor(future);
			assertSuccess(future);
			Object result = future.result();
			// Rest responses do not share a common class. We just use reflection to extract the uuid from the response pojo
			Object uuidObject = result.getClass()
					.getMethod("getUuid")
					.invoke(result);
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

	public static String getSimpleQuery(String text) throws JSONException {
		QueryBuilder qb = QueryBuilders.queryStringQuery(text);
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
		RangeQueryBuilder range = QueryBuilders.rangeQuery(fieldName)
				.from(from)
				.to(to);
		return "{ \"query\": " + range.toString() + "}";
	}

}
