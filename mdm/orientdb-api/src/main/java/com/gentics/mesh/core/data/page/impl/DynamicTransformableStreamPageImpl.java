package com.gentics.mesh.core.data.page.impl;

import java.util.function.Predicate;
import java.util.stream.Stream;

import com.gentics.mesh.core.data.HibTransformableElement;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.PagingParameters;

public class DynamicTransformableStreamPageImpl<T extends HibTransformableElement<? extends RestModel>> extends DynamicStreamPageImpl<T> {
	public DynamicTransformableStreamPageImpl(Stream<? extends T> stream, PagingParameters pagingInfo) {
		super(stream, pagingInfo);
	}

	public DynamicTransformableStreamPageImpl(Stream<? extends T> stream, PagingParameters pagingInfo, Predicate<T> filter) {
		super(stream, pagingInfo, filter);
	}
}
