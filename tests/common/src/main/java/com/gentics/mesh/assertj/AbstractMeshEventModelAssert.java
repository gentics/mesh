package com.gentics.mesh.assertj;

import org.assertj.core.api.AbstractAssert;

public abstract class AbstractMeshEventModelAssert<S extends AbstractMeshEventModelAssert<S, A>, A> extends AbstractAssert<S, A> {

	protected AbstractMeshEventModelAssert(A actual, Class<?> selfType) {
		super(actual, selfType);
	}

}
