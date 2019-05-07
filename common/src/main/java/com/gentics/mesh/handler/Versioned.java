package com.gentics.mesh.handler;

import com.gentics.mesh.context.InternalActionContext;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;

import java.util.function.Supplier;

/**
 * A wrapper that helps getting different objects for different versions.
 * @param <T>
 */
public class Versioned<T> {
	private final RangeMap<Integer, Supplier<T>> versions;
	private Versioned(RangeMap<Integer, Supplier<T>> versions) {
		this.versions = versions;
	}

	/**
	 * Executes an action if the requested version is equal or above {@code version}.
	 * @param version
	 * @param context The context of the request
	 * @param runnable The code to be exectued
	 */
	public static void doSince(int version, InternalActionContext context, Runnable runnable) {
		if (context.getApiVersion() >= version) {
			runnable.run();
		}
	}

	/**
	 * Gets the wrapped object for the given version.
	 * @param version
	 * @return
	 */
	public T forVersion(int version) {
		return versions.get(version).get();
	}

	/**
	 * Gets the wrapped object in the given context.
	 * @param ctx
	 * @return
	 */
	public T forVersion(InternalActionContext ctx) {
		return forVersion(ctx.getApiVersion());
	}


	/**
	 * Return the {@code value} argument when the requested version is equal or above the {@code version} argument.
	 * @param version
	 * @param value
	 * @return
	 */
	public static <T> Builder<T> since(int version, T value) {
		return new Builder<T>().since(version, value);
	}

	/**
	 * Return the value supplied by the {@code supplier} argument when the
	 * requested version is equal or above the {@code version} argument.
	 * @param version
	 * @param supplier
	 * @return
	 */
	public static <T> Builder<T> since(int version, Supplier<T> supplier) {
		return new Builder<T>().since(version, supplier);
	}

	/**
	 * Return the {@code value} argument when the requested version is equal to the {@code version} argument.
	 * @param version
	 * @param value
	 * @return
	 */
	public static <T> Builder<T> forVersion(int version, T value) {
		return new Builder<T>().forVersion(version, value);
	}

	/**
	 * Return the value supplied by the {@code supplier} argument when the
	 * requested version is equal to the {@code version} argument.
	 * @param version
	 * @param supplier
	 * @return
	 */
	public static <T> Builder<T> forVersion(int version, Supplier<T> supplier) {
		return new Builder<T>().forVersion(version, supplier);
	}

	public static class Builder<T> {
		private final RangeMap<Integer, Supplier<T>> map = TreeRangeMap.create();

		/**
		 * Return the {@code value} argument when the requested version is equal or above the {@code version} argument.
		 * @param version
		 * @param value
		 * @return
		 */
		public Builder<T> since(int version, T value) {
			return since(version, ConstantSupplier.of(value));
		}

		/**
		 * Return the value supplied by the {@code supplier} argument when the
		 * requested version is equal or above the {@code version} argument.
		 * @param version
		 * @param supplier
		 * @return
		 */
		public Builder<T> since(int version, Supplier<T> supplier) {
			map.put(Range.atLeast(version), supplier);
			return this;
		}

		/**
		 * Return the {@code value} argument when the requested version is equal to the {@code version} argument.
		 * @param version
		 * @param value
		 * @return
		 */
		public Builder<T> forVersion(int version, T value) {
			return forVersion(version, ConstantSupplier.of(value));
		}

		/**
		 * Return the value supplied by the {@code supplier} argument when the
		 * requested version is equal to the {@code version} argument.
		 * @param version
		 * @param supplier
		 * @return
		 */
		public Builder<T> forVersion(int version, Supplier<T> supplier) {
			map.put(Range.singleton(version), supplier);
			return this;
		}

		/**
		 * Build the versioned object.
		 * @return
		 */
		public Versioned<T> build() {
			return new Versioned<>(map);
		}
	}

	private static class ConstantSupplier<T> implements Supplier<T> {
		private final T value;
		private ConstantSupplier(T value) {
			this.value = value;
		}

		public static <T> ConstantSupplier<T> of(T value) {
			return new ConstantSupplier<>(value);
		}

		@Override
		public T get() {
			return value;
		}
	}
}
