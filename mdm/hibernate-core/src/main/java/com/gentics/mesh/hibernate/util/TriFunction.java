package com.gentics.mesh.hibernate.util;

/**
 * A function that takes three parameters and return another object
 */
@FunctionalInterface
public interface TriFunction<T, U, V, R> {
	R apply(T var1, U var2, V var3);
}
