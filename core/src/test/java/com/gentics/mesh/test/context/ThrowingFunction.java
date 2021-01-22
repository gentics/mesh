package com.gentics.mesh.test.context;

public interface ThrowingFunction<I extends Object, O extends Object, E extends Throwable> {

	O apply(I inputData) throws E;
}
