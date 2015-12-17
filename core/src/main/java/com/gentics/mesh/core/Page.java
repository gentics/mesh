package com.gentics.mesh.core;

import java.util.Iterator;
import java.util.List;

import com.gentics.mesh.core.rest.common.AbstractListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;

public class Page<T> implements Iterable<T> {

	private List<? extends T> wrappedList;
	private int totalElements;
	private int numberOfElements;
	private int pageNumber;
	private int totalPages;
	private int perPage;

	public Page(List<? extends T> wrappedList, int totalElements, int pageNumber, int totalPages, int numberOfElements, int perPage) {
		this.wrappedList = wrappedList;
		this.totalElements = totalElements;
		this.pageNumber = pageNumber;
		this.totalPages = totalPages;
		this.numberOfElements = numberOfElements;
		this.perPage = perPage;
	}

	@Override
	public Iterator<T> iterator() {
		return (Iterator<T>) wrappedList.iterator();
	}

	public int getSize() {
		return wrappedList.size();
	}

	public int getTotalElements() {
		return totalElements;
	}

	public int getNumber() {
		return pageNumber;
	}

	public int getTotalPages() {
		return totalPages;
	}

	public int getNumberOfElements() {
		return numberOfElements;
	}

	public long getPerPage() {
		return perPage;
	}

	/**
	 * Set the paging parameters into the given list response by examining the given page.
	 * 
	 * @param response
	 *            List response that will be updated
	 * @param page
	 *            Page that will be used to extract the paging parameters
	 */
	public void setPaging(AbstractListResponse<?> response) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(getNumber());
		info.setPageCount(getTotalPages());
		info.setPerPage(getPerPage());
		info.setTotalCount(getTotalElements());
	}
}
