package com.gentics.mesh.core.data.page.impl;

import java.util.List;

import com.gentics.mesh.core.data.page.Page;

public class WrappedPageImpl<T> implements Page<T> {

	protected List<? extends T> wrappedList;

	protected Page<?> wrappedPage;

	public WrappedPageImpl() {
	}

	public WrappedPageImpl(List<T> elements, Page<?> page) {
		this.wrappedList = elements;
		this.wrappedPage = page;
	}

	@Override
	public int getPerPage() {
		return wrappedPage.getPerPage();
	}

	@Override
	public long getPageCount() {
		return wrappedPage.getPageCount();
	}

	@Override
	public long getNumber() {
		return wrappedPage.getNumber();
	}

	@Override
	public long getTotalElements() {
		return wrappedPage.getTotalElements();
	}

	@Override
	public List<? extends T> getWrappedList() {
		return wrappedList;
	}

	@Override
	public boolean hasNextPage() {
		return wrappedPage.hasNextPage();
	}
}
