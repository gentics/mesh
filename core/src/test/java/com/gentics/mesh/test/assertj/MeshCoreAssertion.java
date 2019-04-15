package com.gentics.mesh.test.assertj;

import com.gentics.mesh.search.verticle.ElasticsearchProcessVerticle;
import com.gentics.mesh.test.context.MeshTestContext;

public final class MeshCoreAssertion {

	public static MeshTestContextAssert assertThat(MeshTestContext actual) {
		return new MeshTestContextAssert(actual);
	}

	public static SearchVerticleAssert assertThat(ElasticsearchProcessVerticle actual) {
		return new SearchVerticleAssert(actual);
	}
}
