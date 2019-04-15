package com.gentics.mesh.test.assertj;

import com.gentics.mesh.search.verticle.ElasticsearchProcessVerticle;
import org.assertj.core.api.AbstractAssert;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;

public class SearchVerticleAssert extends AbstractAssert<SearchVerticleAssert, ElasticsearchProcessVerticle> {
	protected SearchVerticleAssert(ElasticsearchProcessVerticle actual) {
		super(actual, SearchVerticleAssert.class);
	}

	public SearchVerticleAssert isIdle() {
		assertThat(actual.getIdleChecker().isIdle()).as("Search is idle").isTrue();
		return this;
	}
}
