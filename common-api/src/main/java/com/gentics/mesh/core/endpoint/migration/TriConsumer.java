package com.gentics.mesh.core.endpoint.migration;

public interface TriConsumer<T1, T2, T3> {

	/**
	 * Performs this operation on the given arguments.
	 *
	 * @param t1 the first input argument
	 * @param t2 the second input argument
	 * @param t3 the third input argument
	 */
	void accept(T1 t1, T2 t2, T3 t3);
}
