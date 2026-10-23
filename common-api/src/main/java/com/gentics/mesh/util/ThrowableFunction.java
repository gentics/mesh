package com.gentics.mesh.util;

/**
 * A function processing I into O, possibly throwing T
 * 
 * @param <I> input type
 * @param <O> output type
 * @param <T> throwable type
 */
@FunctionalInterface
public interface ThrowableFunction<I, O, T extends Throwable> {

	O apply(I input) throws T;
}
