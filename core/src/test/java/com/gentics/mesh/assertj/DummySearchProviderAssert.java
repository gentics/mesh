package com.gentics.mesh.assertj;

import static org.junit.Assert.assertEquals;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.search.DummySearchProvider;

public class DummySearchProviderAssert extends AbstractAssert<DummySearchProviderAssert, DummySearchProvider> {

	protected DummySearchProviderAssert(DummySearchProvider actual) {
		super(actual, DummySearchProviderAssert.class);
	}

	public DummySearchProviderAssert recordedStoreEvents(int count) {
		isNotNull();
		String info = actual.getStoreEvents().keySet().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of store events. Found events: {\n" + info + "\n}", count,
				actual.getStoreEvents().size());
		return this;
	}

	public DummySearchProviderAssert recordedDeleteEvents(int count) {
		isNotNull();
		String info = actual.getDeleteEvents().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		int found = actual.getDeleteEvents().size();
		if (found != count) {
			failWithMessage("The search provider did not record the correct amount {%s} of delete events. Found {%s} events: {\n" + info + "\n}",
					count, found);
		}
		return this;
	}

	public DummySearchProviderAssert hasNoStoreEvents() {
		return recordedStoreEvents(0);
	}
}
