package com.gentics.mesh.util;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.gentics.mesh.api.common.SortOrder;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.PagingParameters;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;

/**
 * This class contains a collection of traversal methods that can be used for pagination and other traversals.
 */
public final class TraversalHelper {

	/**
	 * Create a page result for the given traversal and the specified paging parameters. Due to Tinkerpop Gremlin limitation it is needed to manually duplicate
	 * the traversals. TP 3.x will be able to reuse existing traversals.
	 * 
	 * @param traversal
	 *            Base traversal that is used to find the affected elements
	 * @param sortBy
	 *            Order by element property (eg. name, creator..). When null no extra sorting will be applied.
	 * @param order
	 *            Sortorder
	 * @param page
	 *            Page that is currently selected
	 * @param pageSize
	 *            Page size that is used to calculate the skip item amount
	 * @param perPage
	 *            Per page parameter
	 * @param classOfT
	 *            Class that used to map the ferma objects that were found for the page query
	 * @return
	 * @throws InvalidArgumentException
	 */
	private static <T extends TransformableElement<? extends RestModel>> Page<T> getPagedResult(VertexTraversal<?, ?, ?> traversal, String sortBy,
			SortOrder order, int page, int pageSize, int perPage, Class<T> classOfT) throws InvalidArgumentException {

		if (page < 1) {
			throw new GenericRestException(BAD_REQUEST, "error_page_parameter_must_be_positive", String.valueOf(page));
		}
		if (pageSize < 0) {
			throw new GenericRestException(BAD_REQUEST, "error_pagesize_parameter", String.valueOf(pageSize));
		}

		// Internally we start with page 0 in order to comply with the tinkerpop range traversal values which start with 0.
		// External (for the enduser) all pages start with 1.
		page = page - 1;

		long low = page * pageSize - 1;
		long upper = low + pageSize;

		if (pageSize == 0) {
			low = 0;
			upper = 0;
		}

		Iterator<VertexFrame> iterator = traversal.iterator();
		long count = 0;
		List<T> elementsOfPage = new ArrayList<>();
		while (iterator.hasNext()) {
			VertexFrame element = iterator.next();
			// Only add those vertices to the list which are within the bounds of the requested page
			if (count > low && count <= upper) {
				elementsOfPage.add(element.reframeExplicit(classOfT));
			}
			count++;
		}

		// The totalPages of the list response must be zero if the perPage parameter is also zero.
		long totalPages = 0;
		if (perPage != 0) {
			totalPages = (long) Math.ceil(count / (double) (perPage));
		}

		// Internally the page size was reduced. We need to increment it now that we are finished.
		return new PageImpl<T>(elementsOfPage, count, ++page, totalPages, elementsOfPage.size(), perPage);

	}

	/**
	 * Return a paged result for the given traversal and paging parameters.
	 * 
	 * @param traversal
	 * @param pagingInfo
	 * @param classOfT
	 * @return
	 * @throws InvalidArgumentException
	 */
	public static <T extends TransformableElement<? extends RestModel>> Page<T> getPagedResult(VertexTraversal<?, ?, ?> traversal,
			PagingParameters pagingInfo, Class<T> classOfT) throws InvalidArgumentException {
		return getPagedResult(traversal, pagingInfo.getSortBy(), pagingInfo.getOrder(), pagingInfo.getPage(), pagingInfo.getPerPage(),
				pagingInfo.getPerPage(), classOfT);
	}

	/**
	 * Simple debug method for a vertex traversal. All vertices will be printed out. Don't use this code for production.
	 * 
	 * @param traversal
	 */
	public static void debug(VertexTraversal<?, ?, ?> traversal) {
		for (MeshVertexImpl v : traversal.toListExplicit(MeshVertexImpl.class)) {
			System.out.println(v.getProperty("name") + " type: " + v.getFermaType() + " json: " + v.toJson());

		}
	}

	/**
	 * Simple debug method for a edge traversal. All edges will be printed out. Don't use this code for production.
	 * 
	 * @param traversal
	 */
	public static void debug(EdgeTraversal<?, ?, ?> traversal) {
		for (MeshEdgeImpl e : traversal.toListExplicit(MeshEdgeImpl.class)) {
			System.out.println(e.getElement().getId() + "from " + e.inV().next() + " to " + e.outV().next());
			System.out.println(e.getLabel() + " type: " + e.getFermaType() + " json: " + e.toJson());
		}
	}

	/**
	 * Simple debug method for printing all existing vertices.
	 */
	public static void printDebugVertices() {
		for (VertexFrame frame : Database.getThreadLocalGraph().v()) {
			System.out.println(
					frame.getId() + " " + frame.getProperty("ferma_type") + " " + frame.getProperty("name") + " " + frame.getProperty("uuid"));
		}

	}

}
