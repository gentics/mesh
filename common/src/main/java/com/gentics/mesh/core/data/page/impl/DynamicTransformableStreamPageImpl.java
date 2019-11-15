package com.gentics.mesh.core.data.page.impl;

import java.util.function.Predicate;
import java.util.stream.Stream;

import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.PagingParameters;

public class DynamicTransformableStreamPageImpl<T extends TransformableElement<? extends RestModel>> extends DynamicStreamPageImpl<T> implements TransformablePage<T> {
	public DynamicTransformableStreamPageImpl(Stream<? extends T> stream, PagingParameters pagingInfo) {
		super(stream, pagingInfo);
	}

	public DynamicTransformableStreamPageImpl(Stream<? extends T> stream, PagingParameters pagingInfo, Predicate<T> filter) {
		super(stream, pagingInfo, filter);
	}
}
