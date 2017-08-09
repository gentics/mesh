package com.gentics.mesh.core.data.page.impl;

import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import com.gentics.mesh.parameter.PagingParameters;

public class DynamicStreamPageImpl<T> extends AbstractDynamicPage<T> {

	public DynamicStreamPageImpl(Stream<T> stream, PagingParameters pagingInfo) {
		super(pagingInfo);
		init(stream);
	}

	private void init(Stream<T> stream) {

		AtomicLong pageCounter = new AtomicLong();
		visibleItems = stream

				.map(item -> {
					totalCounter.incrementAndGet();
					return item;
				})

				// Apply paging - skip to lower bounds
				.skip(lowerBound)

				.map(item -> {
					// Only add elements to the list if those elements are part of selected the page
					long elementsInPage = pageCounter.get();
					if (elementsInPage < perPage) {
						elementsOfPage.add(item);
						pageCounter.incrementAndGet();
					} else {
						pageFull.set(true);
						hasNextPage.set(true);
					}
					return item;
				})

				.iterator();

	}

}
