package com.gentics.mesh.core.data.page.impl;

import com.gentics.mesh.parameter.PagingParameters;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class DynamicStreamPageImpl<T> extends AbstractDynamicPage<T> {

	public DynamicStreamPageImpl(Stream<? extends T> stream, PagingParameters pagingInfo) {
		super(pagingInfo);
		init(stream);
	}

	public DynamicStreamPageImpl(Stream<? extends T> stream, PagingParameters pagingInfo, Predicate<T> filter) {
		super(pagingInfo);
		init(stream.filter(filter));
	}

	private void init(Stream<? extends T> stream) {
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
