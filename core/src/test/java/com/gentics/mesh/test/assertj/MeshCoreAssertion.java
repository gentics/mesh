package com.gentics.mesh.test.assertj;

import com.gentics.mesh.test.context.MeshTestContext;

public final class MeshCoreAssertion {

	public static MeshTestContextAssert assertThat(MeshTestContext actual) {
		return new MeshTestContextAssert(actual);
	}

}
