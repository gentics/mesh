package com.gentics.mesh.core;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;

import com.gentics.mesh.rest.client.MeshRestClientMessageException;

import io.netty.handler.codec.http.HttpResponseStatus;

/**
 * Test util functions
 * 
 * @author plyhun
 *
 */
public final class HibernateTestUtils {

	private HibernateTestUtils() {
	}

	/**
	 * Pick a free local port.
	 * 
	 * @param stable
	 * @return
	 * @throws IOException
	 */
	public static int pickLocalPort(boolean stable) {
		if (stable) {
			return 65533;
		} else {
			try (ServerSocket socket = new ServerSocket(0)) {
				return socket.getLocalPort();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	/**
	 * Check whether the throwable is a {@link MeshRestClientMessageException} with {@link MeshRestClientMessageException#getStatusCode()} {@link HttpResponseStatus#CONFLICT}.
	 * @param t throwable
	 * @return true for conflict errors, false otherwise
	 */
	public static boolean isConflict(Throwable t) {
		return isResponseStatus(t, HttpResponseStatus.CONFLICT);
	}

	/**
	 * Check whether the throwable is a {@link MeshRestClientMessageException} with the given {@link MeshRestClientMessageException#getStatusCode()}.
	 * @param t throwable
	 * @param status status code in question
	 * @return true, iff the status code matches
	 */
	public static boolean isResponseStatus(Throwable t, HttpResponseStatus status) {
		return getMeshRestClientMessageException(t).map(meshException -> meshException.getStatusCode() == status.code()).orElse(false);
	}

	/**
	 * Get the optional {@link MeshRestClientMessageException} instance wrapped in the given {@link Throwable}.
	 * @param t throwable
	 * @return optional MeshRestClientMessageException
	 */
	public static Optional<MeshRestClientMessageException> getMeshRestClientMessageException(Throwable t) {
		if (t instanceof MeshRestClientMessageException) {
			MeshRestClientMessageException meshException = ((MeshRestClientMessageException) t);
			return Optional.of(meshException);
		} else if (t.getCause() != null && t.getCause() != t) {
			return getMeshRestClientMessageException(t.getCause());
		} else {
			return Optional.empty();
		}
	}
}
