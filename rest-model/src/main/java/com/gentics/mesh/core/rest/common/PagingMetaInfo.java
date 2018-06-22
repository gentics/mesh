package com.gentics.mesh.core.rest.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/**
 * Paging meta info model.
 */
public class PagingMetaInfo {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Number of the current page.")
	private long currentPage;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Number of elements which can be included in a single page.")
	private Long perPage;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Number of the pages which can be found for the given per page count.")
	private long pageCount;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Number of all elements which could be found.")
	private long totalCount;

	public PagingMetaInfo() {
	}

	/**
	 * Return the current page number.
	 * 
	 * @return Current page number
	 */
	public long getCurrentPage() {
		return currentPage;
	}

	/**
	 * Set the current page number.
	 * 
	 * @param currentPage
	 *            Current page number
	 * @return Fluent API
	 */
	public PagingMetaInfo setCurrentPage(long currentPage) {
		this.currentPage = currentPage;
		return this;
	}

	/**
	 * Return the amount of items that should be included in one page.
	 * 
	 * @return Per page count
	 */
	public Long getPerPage() {
		return perPage;
	}

	/**
	 * Set the per page count.
	 * 
	 * @param perPage
	 *            Per page count
	 * @return Fluent API
	 */
	public PagingMetaInfo setPerPage(long perPage) {
		this.perPage = perPage;
		return this;
	}

	/**
	 * Return the total page count.
	 * 
	 * @return Total page count
	 */
	public long getPageCount() {
		return pageCount;
	}

	/**
	 * Set the total page count.
	 * 
	 * @param pageCount
	 *            Total page count
	 * @return Fluent API
	 */
	public PagingMetaInfo setPageCount(long pageCount) {
		this.pageCount = pageCount;
		return this;
	}

	/**
	 * Return the total element count.
	 * 
	 * @return Total element count
	 */
	public long getTotalCount() {
		return totalCount;
	}

	/**
	 * Set the total element count.
	 * 
	 * @param totalCount
	 *            Total element count
	 * @return Fluent API
	 */
	public PagingMetaInfo setTotalCount(long totalCount) {
		this.totalCount = totalCount;
		return this;
	}

}
