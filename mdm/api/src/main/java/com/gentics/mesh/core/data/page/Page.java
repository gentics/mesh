package com.gentics.mesh.core.data.page;

import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;

/**
 * A page is the result of a query which returns paged data. Each page may contain multiple items. A page can be transformed into a rest response.
 */
public interface Page<T> extends Iterable<T> {

	/**
	 * Return the per page parameter value.
	 * 
	 * @return
	 */
	Long getPerPage();

	/**
	 * Return the total amount of pages which the resources that provided this page could return.
	 * 
	 * @return
	 */
	long getPageCount();

	/**
	 * Return the current page number.
	 * 
	 * @return
	 */
	long getNumber();

	/**
	 * Return the total item count which the resource that provided this page could return.
	 * 
	 * @return
	 */
	long getTotalElements();

	/**
	 * Return the number of elements which are currently contained within the page.
	 * 
	 * @return
	 */
	default int getSize() {
		return getWrappedList().size();
	}

	/**
	 * Set the paging parameters into the given list response by examining the given page.
	 * 
	 * @param response
	 *            List response that will be updated
	 */
	default void setPaging(ListResponse<?> response) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(getNumber());
		info.setPageCount(getPageCount());
		info.setPerPage(getPerPage());
		info.setTotalCount(getTotalElements());
	}

	/**
	 * Returns the wrapped list which contains the results.
	 * 
	 * @return
	 */
	List<? extends T> getWrappedList();

	/**
	 * Return the iterator over the elements of the page.
	 * 
	 * @return
	 */
	@Override
	default Iterator<T> iterator() {
		return (Iterator<T>) getWrappedList().iterator();
	}

	/**
	 * Return the results stream.
	 * 
	 * @return
	 */
	default Stream<? extends T> stream() {
		return getWrappedList().stream();
	}

	/**
	 * Check whether there would be another page.
	 * 
	 * @return
	 */
	boolean hasNextPage();

	/**
	 * Check whether there would be a previous page.
	 */
	default boolean hasPreviousPage() {
		return getNumber() > 1;
	}

}
