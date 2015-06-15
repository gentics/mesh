package com.gentics.mesh.core;

import java.util.Iterator;
import java.util.List;

public class Page<T> implements Iterable<T> {

	private List<? extends T> wrappedList;
	private int totalElements;
	private int numberOfElements;
	private int pageNumber;
	private int totalPages;

	public Page(List<? extends T> wrappedList, int totalElements, int pageNumber, int totalPages, int numberOfElements) {
		this.wrappedList = wrappedList;
		this.totalElements = totalElements;
		this.pageNumber = pageNumber;
		this.totalPages = totalPages;
		this.numberOfElements = numberOfElements;
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
}
