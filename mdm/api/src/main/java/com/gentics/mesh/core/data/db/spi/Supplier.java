package com.gentics.mesh.core.data.db.spi;

public interface Supplier<T> {
	T get() throws Exception;
}
