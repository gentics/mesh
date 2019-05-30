package com.gentics.mesh.graphdb.spi;

public interface Supplier<T> {
	T get() throws Exception;
}
