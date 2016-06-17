package com.gentics.mesh.util;

import static com.gentics.mesh.api.common.SortOrder.DESCENDING;
import static com.gentics.mesh.api.common.SortOrder.UNSORTED;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.api.common.SortOrder;
import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;

/**
 * This class contains a collection of traversal methods that can be used for pagination and other traversals.
 */
public final class TraversalHelper {

	/**
	 * Create a page result for the given traversal and the specified paging parameters. Due to Tinkerpop Gremlin limitation it is needed to manually duplicate
	 * the traverals. TP 3.x will be able to reuse existing traversals.
	 * 
	 * @param traversal
	 *            Base traversal that is used to find the affected elements
	 * @param countTraversal
	 *            Base traversal that is used to find the affected element (used for counting)
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
	private static <T extends TransformableElement<? extends RestModel>> PageImpl<T> getPagedResult(VertexTraversal<?, ?, ?> traversal,
			VertexTraversal<?, ?, ?> countTraversal, String sortBy, SortOrder order, int page, int pageSize, int perPage, Class<T> classOfT)
					throws InvalidArgumentException {

		if (page < 1) {
			throw new GenericRestException(BAD_REQUEST, "error_page_parameter_must_be_positive", String.valueOf(page));
		}
		if (pageSize < 0) {
			throw new GenericRestException(BAD_REQUEST, "error_pagesize_parameter", String.valueOf(pageSize));
		}

		// Internally we start with page 0
		page = page - 1;

		int low = page * pageSize;
		int upper = low + pageSize - 1;

		if (pageSize == 0) {
			low = 0;
			upper = 0;
		}

		int count = (int) countTraversal.count();

		List<? extends T> list = new ArrayList<>();
		if (pageSize != 0) {
			// Only add the filter to the pipeline when the needed parameters were correctly specified.
			if (order != UNSORTED && sortBy != null) {
				traversal = traversal.order((VertexFrame f1, VertexFrame f2) -> {
					if (order == DESCENDING) {
						VertexFrame tmp = f1;
						f1 = f2;
						f2 = tmp;
					}
					return f2.getProperty(sortBy).equals(f1.getProperty(sortBy)) ? 1 : 0;
				});
			}

			list = traversal.range(low, upper).toListExplicit(classOfT);
		}

		int totalPages = (int) Math.ceil(count / (double) pageSize);
		// Cap totalpages to 1
		if (totalPages == 0) {
			totalPages = 1;
		}

		// Internally the page size was reduced. We need to increment it now that we are finished.
		return new PageImpl<T>(list, count, ++page, totalPages, list.size(), perPage);

	}

	/**
	 * Return a paged result for the given traversal and paging parameters.
	 * 
	 * @param traversal
	 * @param countTraversal
	 * @param pagingInfo
	 * @param classOfT
	 * @return
	 * @throws InvalidArgumentException
	 */
	public static <T extends TransformableElement<? extends RestModel>> PageImpl<T> getPagedResult(VertexTraversal<?, ?, ?> traversal,
			VertexTraversal<?, ?, ?> countTraversal, PagingParameters pagingInfo, Class<T> classOfT) throws InvalidArgumentException {
		return getPagedResult(traversal, countTraversal, pagingInfo.getSortBy(), pagingInfo.getOrder(), pagingInfo.getPage(), pagingInfo.getPerPage(),
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
