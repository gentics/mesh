package com.gentics.mesh.core.rest.common;

/**
 * Paging meta info model.
 */
public class PagingMetaInfo {

	private long currentPage;

	private long perPage;

	private long pageCount;

	private long totalCount;

	public PagingMetaInfo() {
	}

	/**
	 * Return the current page number.
	 * 
	 * @return
	 */
	public long getCurrentPage() {
		return currentPage;
	}

	/**
	 * Set the current page number.
	 * 
	 * @param currentPage
	 */
	public void setCurrentPage(long currentPage) {
		this.currentPage = currentPage;
	}

	/**
	 * Return the amount of items that should be included in one page.
	 * 
	 * @return
	 */
	public long getPerPage() {
		return perPage;
	}

	/**
	 * Set the per page count.
	 * 
	 * @param perPage
	 */
	public void setPerPage(long perPage) {
		this.perPage = perPage;
	}

	/**
	 * Return the total page count.
	 * 
	 * @return
	 */
	public long getPageCount() {
		return pageCount;
	}

	/**
	 * Set the total page count.
	 * 
	 * @param pageCount
	 */
	public void setPageCount(long pageCount) {
		this.pageCount = pageCount;
	}

	/**
	 * Return the total element count.
	 * 
	 * @return
	 */
	public long getTotalCount() {
		return totalCount;
	}

	/**
	 * Set the total element count.
	 * 
	 * @param totalCount
	 */
	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}

}
