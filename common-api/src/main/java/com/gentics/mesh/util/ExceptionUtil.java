package com.gentics.mesh.util;

import java.util.concurrent.Callable;

/**
 * Various utility functions regarding Exceptions.
 */
public final class ExceptionUtil {
	private ExceptionUtil() {
	}

	/**
	 * Executes a callable method. Any exceptions thrown will be wrapped in a {@link RuntimeException} which then will be thrown.
	 *
	 * @param callable
	 * @param <T>
	 * @return
	 */
	public static <T> T wrapException(Callable<T> callable) {
		try {
			return callable.call();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Throws the passed throwable.
	 * If the throwable is not a {@link RuntimeException}, it will be wrapped in one and then thrown.
	 *
	 * @param throwable
	 * @return
	 */
	public static RuntimeException rethrow(Throwable throwable) {
		if (throwable instanceof RuntimeException) {
			throw ((RuntimeException) throwable);
		} else {
			throw new RuntimeException(throwable);
		}
	}
}
