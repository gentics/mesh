package com.gentics.mesh.plugin.comment.util;

import java.util.concurrent.Callable;

public class BasicUtil {

	private BasicUtil() {
	}

	/**
	 * Calls the supplier and returns its value. If an exception is thrown, it will be wrapped in
	 * a {@link RuntimeException} and that will be rethrown.
	 */
	public static <T> T propagateError(Callable<T> supplier) {
		try {
			return supplier.call();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
