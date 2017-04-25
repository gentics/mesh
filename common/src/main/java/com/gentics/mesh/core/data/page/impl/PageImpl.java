package com.gentics.mesh.core.data.page.impl;

import java.util.Iterator;
import java.util.List;

import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;

/**
 * @see Page
 * @param <T>
 */
public class PageImpl<T> implements Iterable<T>, Page<T> {

	protected List<? extends T> wrappedList;
	protected long totalElements;
	protected long pageNumber;
	protected long totalPages;
	protected int perPage;

	/**
	 * Construct a new page
	 * 
	 * @param wrappedList
	 *            List which yields the element within the page
	 * @param totalElements
	 *            Total element which could be found
	 * @param pageNumber
	 *            Number of the page
	 * @param totalPages
	 *            Total amount of pages
	 * @param perPage
	 *            Number of element per page
	 */
	public PageImpl(List<? extends T> wrappedList, long totalElements, long pageNumber, long totalPages, int perPage) {
		this.wrappedList = wrappedList;
		this.totalElements = totalElements;
		this.pageNumber = pageNumber;
		this.totalPages = totalPages;
		this.perPage = perPage;
	}

	@Override
	public Iterator<T> iterator() {
		return (Iterator<T>) wrappedList.iterator();
	}

	@Override
	public int getSize() {
		return wrappedList.size();
	}

	@Override
	public long getTotalElements() {
		return totalElements;
	}

	@Override
	public long getNumber() {
		return pageNumber;
	}

	@Override
	public long getPageCount() {
		return totalPages;
	}

	@Override
	public int getPerPage() {
		return perPage;
	}

	@Override
	public void setPaging(ListResponse<?> response) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(getNumber());
		info.setPageCount(getPageCount());
		info.setPerPage(getPerPage());
		info.setTotalCount(getTotalElements());
	}

	@Override
	public List<? extends T> getWrappedList() {
		return wrappedList;
	}

}
