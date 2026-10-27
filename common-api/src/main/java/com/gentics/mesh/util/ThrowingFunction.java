package com.gentics.mesh.util;

/**
 * A function processing I into O, possibly throwing T
 * 
 * @param <I> input type
 * @param <O> output type
 * @param <T> throwable type
 */
@FunctionalInterface
public interface ThrowingFunction<I, O, T extends Throwable> {

	/**
	 * Turn input into output, possibly throwing a checked exception
	 * 
	 * @param input
	 * @return
	 * @throws T
	 */
	O apply(I input) throws T;
}
