package com.gentics.mesh.core.db;

public interface Supplier<T> {
	T get() throws Exception;
}
