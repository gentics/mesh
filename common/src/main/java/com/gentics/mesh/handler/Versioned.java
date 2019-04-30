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

	public T forVersion(int version) {
		return versions.get(version).get();
	}

	public T forVersion(InternalActionContext ctx) {
		return forVersion(ctx.getApiVersion());
	}

	public static <T> Builder<T> newVersioned(T baseVersion) {
		return newVersionedFor(1, baseVersion);
	}

	public static <T> Builder<T> newVersioned(Supplier<T> baseVersion) {
		return newVersionedFor(1, baseVersion);
	}

	public static <T> Builder<T> newVersionedFor(int version, T baseVersion) {
		return new Builder<T>().forVersion(version, baseVersion);
	}

	public static <T> Builder<T> newVersionedFor(int version, Supplier<T> baseVersion) {
		return new Builder<T>().forVersion(version, baseVersion);
	}

	public static class Builder<T> {
		private final RangeMap<Integer, Supplier<T>> map = TreeRangeMap.create();

		public Builder<T> since(int version, T value) {
			return since(version, ConstantSupplier.of(value));
		}

		public Builder<T> since(int version, Supplier<T> supplier) {
			map.put(Range.atLeast(version), supplier);
			return this;
		}

		public Builder<T> forVersion(int version, T value) {
			return forVersion(version, ConstantSupplier.of(value));
		}

		public Builder<T> forVersion(int version, Supplier<T> supplier) {
			map.put(Range.singleton(version), supplier);
			return this;
		}

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
