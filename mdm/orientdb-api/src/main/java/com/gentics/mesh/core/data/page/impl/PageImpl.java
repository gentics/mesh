package com.gentics.mesh.core.data.page.impl;

import java.util.Iterator;
import java.util.List;

import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * @see Page
 * @param <T>
 */
public class PageImpl<T> implements Page<T> {

	private List<T> wrappedList;
	private long totalElements;
	private long pageNumber;
	private long totalPages;
	private Long perPage;

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
	@SuppressWarnings("unchecked")
	public PageImpl(List<? extends T> wrappedList, long totalElements, long pageNumber, long totalPages, Long perPage) {
		this.wrappedList = (List<T>)wrappedList;
		this.totalElements = totalElements;
		this.pageNumber = pageNumber;
		this.totalPages = totalPages;
		this.perPage = perPage;
	}

	public PageImpl(List<? extends T> wrappedList, PagingParameters pagingParameters, long totalElements) {
		this(wrappedList, totalElements, pagingParameters.getPage(), getTotalPages(pagingParameters, totalElements), pagingParameters.getPerPage());
	}

	private static long getTotalPages(PagingParameters pagingParameters, long totalElements) {
		Long perPage = pagingParameters.getPerPage();
		if (perPage == null) {
			return 1;
		} else if (perPage == 0) {
			return 0;
		} else {
			return ceilDiv(totalElements, perPage);
		}
	}

	private static long ceilDiv(long x, long y) {
		return Math.floorDiv(x, y) + (x % y == 0 ? 0 : 1);
	}

	@Override
	public Iterator<T> iterator() {
		return wrappedList.iterator();
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
	public Long getPerPage() {
		return perPage;
	}

	@Override
	public List<? extends T> getWrappedList() {
		return wrappedList;
	}

	@Override
	public boolean hasNextPage() {
		return getPageCount() > getNumber();
	}

}
