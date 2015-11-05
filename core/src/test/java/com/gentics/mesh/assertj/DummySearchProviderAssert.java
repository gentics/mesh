package com.gentics.mesh.assertj;

import static org.junit.Assert.assertEquals;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.search.impl.DummySearchProvider;

public class DummySearchProviderAssert extends AbstractAssert<DummySearchProviderAssert, DummySearchProvider> {

	protected DummySearchProviderAssert(DummySearchProvider actual) {
		super(actual, DummySearchProviderAssert.class);
	}

	public DummySearchProviderAssert recordedStoreEvents(int count) {
		isNotNull();
		assertEquals("The search provider did not record the correct amount of store events.", count, actual.getStoreEvents().size());
		return this;
	}
}
