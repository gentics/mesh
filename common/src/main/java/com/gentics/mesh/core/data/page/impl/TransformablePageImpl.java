package com.gentics.mesh.core.data.page.impl;

import java.util.List;

import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.parameter.PagingParameters;

public class TransformablePageImpl<T extends TransformableElement<? extends RestModel>> implements TransformablePage<T> {
	private final List<T> elements;
	private final PagingParameters pagingParameters;

	public TransformablePageImpl(List<T> elements, PagingParameters pagingParameters) {
		this.elements = elements;
		this.pagingParameters = pagingParameters;
	}

	@Override
	public Long getPerPage() {
		return pagingParameters.getPerPage();
	}

	@Override
	public long getPageCount() {
		return (long) Math.ceil((double) elements.size() / pagingParameters.getPerPage());
	}

	@Override
	public long getNumber() {
		return pagingParameters.getPage();
	}

	@Override
	public long getTotalElements() {
		return elements.size();
	}

	@Override
	public List<? extends T> getWrappedList() {
		return elements;
	}

	@Override
	public boolean hasNextPage() {
		return pagingParameters.getPage() < getPageCount();
	}
}
