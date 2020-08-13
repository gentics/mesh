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
}
