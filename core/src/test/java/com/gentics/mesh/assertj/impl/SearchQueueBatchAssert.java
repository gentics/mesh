package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.search.SearchQueueBatch;

public class SearchQueueBatchAssert extends AbstractAssert<SearchQueueBatchAssert, SearchQueueBatch> {

	public SearchQueueBatchAssert(SearchQueueBatch actual) {
		super(actual, SearchQueueBatchAssert.class);
	}

	public SearchQueueBatchAssert hasEntries(int count) {
		isNotNull();
		assertEquals("The search queue did not contain the expected amount of entries.", count, actual.getEntries().size());
		return this;
	}

}
