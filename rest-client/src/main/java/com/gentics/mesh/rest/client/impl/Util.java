package com.gentics.mesh.rest.client.impl;

import java.util.function.Supplier;

public final class Util {
	private Util() {
	}

	/**
	 * A container for a value that is evaluated on demand.
	 * The supplier will be called at most once. After that, the received value is stored and returned on
	 * subsequent calls of {@link Supplier#get()}
	 *
	 * @param supplier
	 * @param <T>
	 * @return
	 */
	public static <T> Supplier<T> lazily(WrappedSupplier<T> supplier) {
		return new Supplier<T>() {
			T value;

			@Override
			public T get() {
				if (value == null) {
					try {
						value = supplier.get();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
				return value;
			}
		};
	}

	interface WrappedSupplier<T> {
		T get() throws Exception;
	}
}
