package com.gentics.mesh.test;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.http.HttpConstants.IF_NONE_MATCH;
import static com.gentics.mesh.test.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import com.gentics.mesh.core.data.i18n.I18NUtil;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.test.context.ClientHandler;
import com.gentics.mesh.test.util.MeshAssert;
import com.gentics.mesh.util.ETag;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.vertx.core.Future;

public final class ClientHelper {

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
		try {
			return handler.handle().toSingle().blockingGet();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Invokes the request and returns the etag for the response.
	 * 
	 * @param handler
	 * @return
	 */
	public static <T> String callETag(ClientHandler<T> handler) {
		String etag = callETagRaw(handler);
		assertNotNull("The etag of the response should not be null", etag);
		return etag;
	}

	public static <T> String callETagRaw(ClientHandler<T> handler) {
		MeshResponse<T> response;
		try {
			response = handler.handle().invoke();
		} catch (Exception e) {
			response = new MeshResponse<>(Future.failedFuture(e));
		}
		latchFor(response);
		assertSuccess(response);
		return ETag.extract(response.getRawResponse().getHeader(ETAG));
	}

	/**
	 * Call the given handler, latch for the future and assert success. Then return the result.
	 * 
	 * @param handler
	 *            handler
	 * @param etag
	 * @param <T>
	 *            type of the returned object
	 * @return result of the future
	 */
	public static <T> String callETag(ClientHandler<T> handler, String etag, boolean isWeak, int statusCode) {
		MeshResponse<T> response;
		try {
			MeshRequest<T> request = handler.handle();
			request.getRequest().putHeader(IF_NONE_MATCH, ETag.prepareHeader(etag, isWeak));
			response = request.invoke();
		} catch (Exception e) {
			response = new MeshResponse<>(Future.failedFuture(e));
		}
		latchFor(response);
		assertSuccess(response);
		int actualStatusCode = response.getRawResponse().statusCode();
		String actualETag = ETag.extract(response.getRawResponse().getHeader(ETAG));
		assertEquals("The response code did not match.", statusCode, actualStatusCode);
		if (statusCode == 304) {
			assertEquals(etag, actualETag);
			assertNull("The response should be null since we got a 304", response.result());
		}
		return actualETag;
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
	 * @return
	 */
	public static <T> MeshRestClientMessageException call(ClientHandler<T> handler, HttpResponseStatus status, String bodyMessageI18nKey,
		String... i18nParams) {
		try {
			handler.handle().toSingle().blockingGet();
			fail("We expected the future to have failed but it succeeded.");
		} catch (RuntimeException | MeshRestClientMessageException e) {
			MeshRestClientMessageException error;
			if (e instanceof RuntimeException) {
				error = (MeshRestClientMessageException) e.getCause();
			} else {
				error = (MeshRestClientMessageException) e;
			}
			if (bodyMessageI18nKey == null) {
				expectFailureMessage(error, status, null);
			} else {
				expectException(error, status, bodyMessageI18nKey, i18nParams);
			}
			if (error instanceof MeshRestClientMessageException) {
				return (MeshRestClientMessageException) e.getCause();
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		return null;
	}

	/**
	 * Call the given handler, latch for the future and expect the given failure in the future.
	 *
	 * @param handler
	 *            handler
	 * @param status
	 *            expected response status
	 * @return
	 */
	public static <T> MeshRestClientMessageException call(ClientHandler<T> handler, HttpResponseStatus status) {
		return call(handler, status, null);
	}

	public static void validateDeletion(Function<Integer, MeshRequest<Void>> deleteOperation, int count) {
		Long successCount = Observable.range(0, count)
			.flatMap(i -> deleteOperation.apply(i).toMaybe()
				.map(ignore -> "dummy")
				.toSingle("dummy")
				.toObservable()
				.onErrorResumeNext(Observable.empty())
			).count().blockingGet();

		assertFalse("We found more than one request that succeeded. Only one of the requests should be able to delete the node.", successCount > 1);
		assertTrue("We did not find a single request which succeeded.", successCount != 0);
	}

	public static void validateFutures(Set<MeshResponse<?>> set) {
		for (MeshResponse<?> future : set) {
			latchFor(future);
			assertSuccess(future);
		}
	}

	public static void assertEqualsSanitizedJson(String msg, String expectedJson, String unsanitizedResponseJson) {
		String sanitizedJson = unsanitizedResponseJson.replaceAll("uuid\":\"[^\"]*\"", "uuid\":\"uuid-value\"");
		assertEquals(msg, expectedJson, sanitizedJson);
	}

	public static void expectFailureMessage(MeshResponse<?> future, HttpResponseStatus status, String message) {
		assertTrue("We expected the future to have failed but it succeeded.", future.failed());
		assertNotNull(future.cause());

		if (future.cause() instanceof MeshRestClientMessageException) {
			MeshRestClientMessageException exception = ((MeshRestClientMessageException) future.cause());
			assertEquals("The status code of the nested exception did not match the expected value.", status.code(), exception.getStatusCode());

			GenericMessageResponse msg = exception.getResponseMessage();
			if (msg != null) {
				assertEquals(message, msg.getMessage());
			} else {
				assertEquals(message, exception.getMessage());
			}
		} else {
			future.cause().printStackTrace();
			fail("Unhandled exception");
		}
	}

	public static void expectException(MeshResponse<?> future, HttpResponseStatus status, String bodyMessageI18nKey, String... i18nParams) {
		Locale en = Locale.ENGLISH;
		String message = I18NUtil.get(en, bodyMessageI18nKey, i18nParams);
		assertNotEquals("Translation for key " + bodyMessageI18nKey + " not found", message, bodyMessageI18nKey);
		expectFailureMessage(future, status, message);
	}

	public static void expectException(Throwable e, HttpResponseStatus status, String bodyMessageI18nKey, String... i18nParams) {
		Locale en = Locale.ENGLISH;
		String message = I18NUtil.get(en, bodyMessageI18nKey, i18nParams);
		assertNotEquals("Translation for key " + bodyMessageI18nKey + " not found", message, bodyMessageI18nKey);
		expectFailureMessage(e, status, message);
	}

	public static void expectFailureMessage(Throwable e, HttpResponseStatus status, String message) {
		if (e instanceof MeshRestClientMessageException) {
			MeshRestClientMessageException exception = ((MeshRestClientMessageException) e);
			assertEquals("The status code of the nested exception did not match the expected value.", status.code(), exception.getStatusCode());

			GenericMessageResponse msg = exception.getResponseMessage();
			if (msg != null) {
				assertEquals(message, msg.getMessage());
			} else {
				assertEquals(message, exception.getMessage());
			}
		} else {
			e.printStackTrace();
			fail("Unhandled exception");
		}
	}
}
