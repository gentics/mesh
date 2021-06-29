package com.gentics.mesh.test;

import com.gentics.mesh.core.data.i18n.I18NUtil;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.rest.client.impl.EmptyResponse;
import com.gentics.mesh.test.context.ClientHandler;
import com.gentics.mesh.util.ETag;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
import io.reactivex.Single;

import java.util.Locale;
import java.util.function.Function;

import static com.gentics.mesh.http.HttpConstants.ETAG;
import static com.gentics.mesh.http.HttpConstants.IF_NONE_MATCH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
			return handler.handle().blockingGet();
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
			response = handler.handle().getResponse().blockingGet();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return ETag.extract(response.getHeader(ETAG).orElse(null));
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
			request.setHeader(IF_NONE_MATCH, ETag.prepareHeader(etag, isWeak));
			response = request.getResponse().blockingGet();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		int actualStatusCode = response.getStatusCode();
		String actualETag = ETag.extract(response.getHeader(ETAG).orElse(null));
		assertEquals("The response code did not match.", statusCode, actualStatusCode);
		if (statusCode == 304) {
			assertEquals(etag, actualETag);
			assertNull("The response should be null since we got a 304", response.getBody());
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
			handler.handle().blockingGet();
			fail("We expected the future to have failed but it succeeded.");
		} catch (RuntimeException | MeshRestClientMessageException e) {
			MeshRestClientMessageException error;
			if (e instanceof RuntimeException) {
				Throwable cause = e.getCause();
				if (cause instanceof MeshRestClientMessageException) {
					error = (MeshRestClientMessageException) e.getCause();
				} else {
					throw (RuntimeException) e;
				}
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
	 * @param bodyMessageI18nKey
	 *            i18n of the expected response message
	 * @param i18nParams
	 *            parameters of the expected response message
	 * @return
	 */
	public static <T> MeshRestClientMessageException call(Single<GenericMessageResponse> request, HttpResponseStatus status,
		String bodyMessageI18nKey,
		String... i18nParams) {
		try {
			request.blockingGet();
			fail("We expected the future to have failed but it succeeded.");
		} catch (RuntimeException e) {
			MeshRestClientMessageException error;
			Throwable cause = e.getCause();
			if (cause instanceof MeshRestClientMessageException) {
				error = (MeshRestClientMessageException) e.getCause();
			} else {
				throw e;
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

	public static void validateDeletion(Function<Integer, MeshRequest<EmptyResponse>> deleteOperation, int count) {
		Long successCount = Observable.range(0, count)
			.flatMap(i -> deleteOperation.apply(i).toMaybe()
				.map(ignore -> "dummy")
				.toSingle("dummy")
				.toObservable()
				.onErrorResumeNext(Observable.empty()))
			.count().blockingGet();

		assertFalse("We found more than one request that succeeded. Only one of the requests should be able to delete the node.", successCount > 1);
		assertTrue("We did not find a single request which succeeded.", successCount != 0);
	}

	public static void expectException(Throwable e, HttpResponseStatus status, String bodyMessageI18nKey, String... i18nParams) {
		Locale en = Locale.ENGLISH;
		String message = I18NUtil.get(en, bodyMessageI18nKey, i18nParams);
		assertNotEquals("Translation for key " + bodyMessageI18nKey + " not found", message, bodyMessageI18nKey);
		expectFailureMessage(e, status, message);
	}

	public static void expectFailureMessage(Throwable e, HttpResponseStatus status, String message) {
		if (e instanceof GenericRestException) {
			GenericRestException exception = ((GenericRestException) e);
			assertEquals("The status code of the nested exception did not match the expected value.", status.code(), exception.getStatus().code());

			if (message != null) {
				Locale en = Locale.ENGLISH;
				String exceptionMessage = I18NUtil.get(en, exception.getI18nKey(), exception.getI18nParameters());
				assertEquals(message, exceptionMessage);
			}
		} else if (e instanceof MeshRestClientMessageException) {
			MeshRestClientMessageException exception = ((MeshRestClientMessageException) e);
			assertEquals("The status code of the nested exception did not match the expected value.", status.code(), exception.getStatusCode());

			if (message != null) {
				GenericMessageResponse msg = exception.getResponseMessage();
				if (msg != null) {
					assertEquals(message, msg.getMessage());
				} else {
					assertEquals(message, exception.getMessage());
				}
			}
		} else {
			e.printStackTrace();
			fail("Unhandled exception");
		}
	}
}
