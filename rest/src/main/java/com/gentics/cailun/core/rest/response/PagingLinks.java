package com.gentics.cailun.core.rest.response;

public class PagingLinks {
	// "/products?page=5&per_page=20"
	private String self;

	// "/products?page=0&per_page=20"
	private String first;

	// "/products?page=4&per_page=20"
	private String previous;

	// "/products?page=6&per_page=20"
	private String next;

	// "/products?page=26&per_page=20"
	private String last;

	public PagingLinks() {
	}

	public String getFirst() {
		return first;
	}

	public void setFirst(String first) {
		this.first = first;
	}

	public String getLast() {
		return last;
	}

	public void setLast(String last) {
		this.last = last;
	}

	public String getNext() {
		return next;
	}

	public void setNext(String next) {
		this.next = next;
	}

	public String getPrevious() {
		return previous;
	}

	public void setPrevious(String previous) {
		this.previous = previous;
	}

	public String getSelf() {
		return self;
	}

	public void setSelf(String self) {
		this.self = self;
	}
}
