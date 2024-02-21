package com.gentics.mesh.core.data.page.impl;

import java.util.function.Predicate;
import java.util.stream.Stream;

import com.gentics.mesh.core.data.HibTransformableElement;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Dynamic streamable page implementation which can handle transformable elements.
 * 
 * @param <T>
 */
public class DynamicTransformableStreamPageImpl<T extends HibTransformableElement<? extends RestModel>> extends DynamicStreamPageImpl<T> {

	public DynamicTransformableStreamPageImpl(Stream<? extends T> stream, PagingParameters pagingInfo) {
		this(stream, pagingInfo, null);
	}

	public DynamicTransformableStreamPageImpl(Stream<? extends T> stream, PagingParameters pagingInfo, Predicate<T> filter) {
		this(stream, pagingInfo, filter, false);
	}

	public DynamicTransformableStreamPageImpl(Stream<? extends T> stream, PagingParameters pagingInfo, Predicate<T> filter, boolean ignorePaging) {
		super(stream, pagingInfo, filter, ignorePaging);
	}
}
