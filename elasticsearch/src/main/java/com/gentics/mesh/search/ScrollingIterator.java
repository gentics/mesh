package com.gentics.mesh.search;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.search.SearchHit;

/**
 * Wrapper for typical elasticsearch {@link SearchResponse} results. The wrapper will use the scroll API to advance the iteration if needed.
 */
public class ScrollingIterator implements Iterator<SearchHit> {

	private Iterator<SearchHit> currentIterator;
	private SearchResponse currentResponse;
	private Client client;

	/**
	 * Create a new iterator.
	 * 
	 * @param client
	 *            Elasticsearch client used to invoke additional queries
	 * @param scrollResp
	 *            Current scroll which will provide the initial results and the scroll reference id.
	 */
	public ScrollingIterator(Client client, SearchResponse scrollResp) {
		this.currentIterator = scrollResp.getHits().iterator();
		this.currentResponse = scrollResp;
		this.client = client;
	}

	@Override
	public boolean hasNext() {
		if (currentIterator.hasNext()) {
			return true;
		}

		if (currentResponse.getHits().getHits().length == 0) {
			return false;
		} else {
			advanceScroll();
			return currentIterator.hasNext();
		}
	}

	/**
	 * Load a new search response using the scrollId of the previous scroll.
	 */
	private void advanceScroll() {
		currentResponse = client.prepareSearchScroll(currentResponse.getScrollId()).setScroll(new TimeValue(60000)).execute().actionGet();
		currentIterator = currentResponse.getHits().iterator();
	}

	@Override
	public SearchHit next() {
		// We need to invoke the hasNext method in order to advance the iterator if needed.
		if (hasNext()) {
			return currentIterator.next();
		} else {
			throw new NoSuchElementException();
		}
	}

}
