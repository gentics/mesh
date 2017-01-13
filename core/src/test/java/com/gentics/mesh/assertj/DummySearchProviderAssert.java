package com.gentics.mesh.assertj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

	/**
	 * Verify that the search provider recorded the given store event.
	 * 
	 * @param indexName
	 * @param indexType
	 * @param documentId
	 * @return Fluent API
	 */
	public DummySearchProviderAssert hasStore(String indexName, String indexType, String documentId) {
		String key = indexName + "-" + indexType + "-" + documentId;
		boolean hasKey = actual.getStoreEvents().containsKey(key);
		if (!hasKey) {
			for (String event : actual.getStoreEvents().keySet()) {
				System.out.println("Recorded store event: " + event);
			}
		}
		assertTrue("The store event could not be found. {" + indexName + "} {" + indexType + "} {" + documentId + "}", hasKey);
		return this;
	}

	/**
	 * Verify that the search provider recorded the given delete event.
	 * 
	 * @param indexName
	 * @param indexType
	 * @param documentId
	 * @return Fluent API
	 */
	public DummySearchProviderAssert hasDelete(String indexName, String indexType, String documentId) {
		String key = indexName + "-" + indexType + "-" + documentId;
		boolean hasKey = actual.getDeleteEvents().contains(key);
		if (!hasKey) {
			for (String event : actual.getDeleteEvents()) {
				System.out.println("Recorded delete event: " + event);
			}
		}
		assertTrue("The delete event could not be found.", hasKey);
		return this;
	}

	/**
	 * Assert that the correct count of events was registered.
	 * 
	 * @param storeEvents
	 * @param deleteEvents
	 * @param dropIndexEvents
	 * @param createIndexEvents
	 * @return Fluent API
	 */
	public DummySearchProviderAssert events(int storeEvents, int deleteEvents, int dropIndexEvents, int createIndexEvents) {
		String storeInfo = actual.getStoreEvents().keySet().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of store events. Found events: {\n" + storeInfo + "\n}", storeEvents,
				actual.getStoreEvents().size());

		String deleteInfo = actual.getDeleteEvents().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of delete events. Found events: {\n" + deleteInfo + "\n}", deleteEvents,
				actual.getDeleteEvents().size());

		String dropInfo = actual.getDropIndexEvents().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of drop index events. Found events: {\n" + dropInfo + "\n}",
				dropIndexEvents, actual.getDropIndexEvents().size());

		String createInfo = actual.getCreateIndexEvents().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of create index events. Found events: {\n" + createInfo + "\n}",
				createIndexEvents, actual.getCreateIndexEvents().size());

		return this;
	}
}
